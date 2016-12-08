/*
 * Copyright 2010-2016 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.jetbrains.kotlin.codegen.coroutines

import org.jetbrains.kotlin.codegen.*
import org.jetbrains.kotlin.codegen.context.ClosureContext
import org.jetbrains.kotlin.codegen.state.GenerationState
import org.jetbrains.kotlin.coroutines.isSuspendLambda
import org.jetbrains.kotlin.descriptors.*
import org.jetbrains.kotlin.descriptors.annotations.Annotations
import org.jetbrains.kotlin.descriptors.impl.SimpleFunctionDescriptorImpl
import org.jetbrains.kotlin.descriptors.impl.ValueParameterDescriptorImpl
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.psi.KtDeclarationWithBody
import org.jetbrains.kotlin.psi.KtElement
import org.jetbrains.kotlin.psi.KtFunctionLiteral
import org.jetbrains.kotlin.resolve.descriptorUtil.module
import org.jetbrains.kotlin.resolve.jvm.AsmTypes
import org.jetbrains.kotlin.resolve.jvm.diagnostics.JvmDeclarationOrigin
import org.jetbrains.kotlin.resolve.jvm.diagnostics.OtherOrigin
import org.jetbrains.kotlin.resolve.jvm.jvmSignature.JvmMethodSignature
import org.jetbrains.kotlin.types.KotlinType
import org.jetbrains.kotlin.types.typeUtil.makeNullable
import org.jetbrains.kotlin.utils.addToStdlib.safeAs
import org.jetbrains.kotlin.utils.singletonOrEmptyList
import org.jetbrains.org.objectweb.asm.Label
import org.jetbrains.org.objectweb.asm.Opcodes
import org.jetbrains.org.objectweb.asm.Type
import org.jetbrains.org.objectweb.asm.commons.InstructionAdapter
import org.jetbrains.org.objectweb.asm.commons.Method


class CoroutineCodegen(
        state: GenerationState,
        element: KtElement,
        private val closureContext: ClosureContext,
        strategy: FunctionGenerationStrategy,
        parentCodegen: MemberCodegen<*>,
        classBuilder: ClassBuilder,
        private val coroutineLambdaDescriptor: FunctionDescriptor
) : ClosureCodegen(state, element, null, closureContext, null, strategy, parentCodegen, classBuilder) {

    private val classDescriptor = closureContext.contextDescriptor

    // protected fun doResume(result, throwable)
    private val doResumeDescriptor =
            SimpleFunctionDescriptorImpl.create(
                    classDescriptor, Annotations.EMPTY, Name.identifier("doResume"), CallableMemberDescriptor.Kind.DECLARATION,
                    funDescriptor.source
            ).apply doResume@{
                initialize(
                        /* receiverParameterType = */ null,
                        classDescriptor.thisAsReceiverParameter,
                        /* typeParameters =   */ emptyList(),
                        listOf(
                              ValueParameterDescriptorImpl(
                                      this@doResume, null, 0, Annotations.EMPTY, Name.identifier("data"),
                                      module.builtIns.nullableAnyType,
                                      /* isDefault = */ false, /* isCrossinline = */ false,
                                      /* isNoinline = */ false, /* isCoroutine = */ false,
                                      /* varargElementType = */ null, SourceElement.NO_SOURCE
                              ),
                              ValueParameterDescriptorImpl(
                                      this@doResume, null, 1, Annotations.EMPTY, Name.identifier("throwable"),
                                      module.builtIns.throwable.defaultType.makeNullable(),
                                      /* isDefault = */ false, /* isCrossinline = */ false,
                                      /* isNoinline = */ false, /* isCoroutine = */ false,
                                      /* varargElementType = */ null, SourceElement.NO_SOURCE
                              )
                        ),
                        module.builtIns.unitType,
                        Modality.FINAL,
                        Visibilities.PROTECTED
                )
            }

    override fun generateClosureBody() {
        for (parameter in allLambdaParameters()) {
            val fieldInfo = parameter.getFieldInfoForCoroutineLambdaParameter()
            v.newField(
                    OtherOrigin(parameter),
                    Opcodes.ACC_PRIVATE,
                    fieldInfo.fieldName,
                    fieldInfo.fieldType.descriptor, null, null
            )
        }

        generateDoResume()

        functionCodegen.generateMethod(JvmDeclarationOrigin.NO_ORIGIN, funDescriptor,
                                       object : FunctionGenerationStrategy.CodegenBased(state) {
                                           override fun doGenerateBody(codegen: ExpressionCodegen, signature: JvmMethodSignature) {
                                                generateInvokeMethod(codegen, signature)
                                           }
                                       })
    }

    // invoke for lambda being passes to builder
    // fun builder(coroutine c: Controller.() -> Continuation<Unit>)
    //
    // This lambda must have a receiver parameter, may have value parameters and returns Continuation<Unit> (`this` instance or a copy of it)
    private fun generateInvokeMethod(codegen: ExpressionCodegen, signature: JvmMethodSignature) {
        val classDescriptor = closureContext.contextDescriptor
        val owner = typeMapper.mapClass(classDescriptor)
        val continuationFieldInfo =
                FieldInfo.createForHiddenField(
                        AsmTypes.COROUTINE_IMPL,
                        AsmTypes.CONTINUATION, CONTINUATION_FIELD_NAME
                )

        val thisInstance = StackValue.thisOrOuter(codegen, classDescriptor, false, false)

        with(codegen.v) {
            // if (controller != null)
            StackValue.field(continuationFieldInfo, thisInstance).put(AsmTypes.OBJECT_TYPE, this)
            val repeated = Label()
            ifnonnull(repeated)

            // first call
            AsmUtil.genAssignInstanceFieldFromParam(continuationFieldInfo, allLambdaParameters().size + 1, this)

            setLabelValue(LABEL_VALUE_BEFORE_FIRST_SUSPENSION)

            // Save lambda parameters to fields
            // 0 - this
            var index = 1
            for (parameter in allLambdaParameters()) {
                val fieldInfoForCoroutineLambdaParameter = parameter.getFieldInfoForCoroutineLambdaParameter()
                AsmUtil.genAssignInstanceFieldFromParam(
                        fieldInfoForCoroutineLambdaParameter, index, this)
                index += fieldInfoForCoroutineLambdaParameter.fieldType.size
            }

            load(0, AsmTypes.OBJECT_TYPE)
            StackValue.putUnitInstance(this)
            aconst(null)
            invokevirtual(className, "doResume", typeMapper.mapSignatureSkipGeneric(doResumeDescriptor).asmMethod.descriptor, false)
            loadSuspendMarker()
            areturn(AsmTypes.OBJECT_TYPE)

            // repeated call
            visitLabel(repeated)
            anew(owner)
            dup()

            // pass closure parameters to constructor
            val constructorParameters = calculateConstructorParameters(typeMapper, closure, owner)
            for (parameter in constructorParameters) {
                StackValue.field(parameter, thisInstance).put(parameter.fieldType, this)
            }

            val constructor = Method("<init>", Type.VOID_TYPE, constructorParameters.map { it.fieldType }.toTypedArray())
            invokespecial(owner.internalName, constructor.name, constructor.descriptor, false)

            // Pass lambda parameters to 'invoke' call on newly constructed object
            index = 1
            for (parameter in signature.valueParameters) {
                load(index, parameter.asmType)
                index += parameter.asmType.size
            }

            // 'invoke' call on freshly constructed coroutine returns receiver itself
            invokevirtual(owner.internalName, signature.asmMethod.name, signature.asmMethod.descriptor, false)
            areturn(AsmTypes.OBJECT_TYPE)
        }
    }

    private fun ExpressionCodegen.initializeCoroutineParameters() {
        for (parameter in allLambdaParameters()) {
            val mappedType = typeMapper.mapType(parameter.type)
            val newIndex = myFrameMap.enter(parameter, mappedType)

            generateLoadField(parameter.getFieldInfoForCoroutineLambdaParameter())
            v.store(newIndex, mappedType)
        }
    }

    private fun allLambdaParameters() =
            coroutineLambdaDescriptor.extensionReceiverParameter.singletonOrEmptyList() + coroutineLambdaDescriptor.valueParameters

    private fun ExpressionCodegen.generateLoadField(fieldInfo: FieldInfo) {
        StackValue.field(fieldInfo, generateThisOrOuter(context.thisDescriptor, false)).put(fieldInfo.fieldType, v)
    }

    private fun ParameterDescriptor.getFieldInfoForCoroutineLambdaParameter() =
            createHiddenFieldInfo(type, COROUTINE_LAMBDA_PARAMETER_PREFIX + (this.safeAs<ValueParameterDescriptor>()?.index ?: ""))

    private fun createHiddenFieldInfo(type: KotlinType, name: String) =
            FieldInfo.createForHiddenField(
                    typeMapper.mapClass(closureContext.thisDescriptor),
                    typeMapper.mapType(type),
                    name
            )

    private fun generateExceptionHandlingBlock(codegen: ExpressionCodegen) {
        with(codegen.v) {
            invokestatic(COROUTINE_MARKER_OWNER, HANDLE_EXCEPTION_MARKER_NAME, "()V", false)
            load(0, AsmTypes.OBJECT_TYPE)

            getfield(AsmTypes.COROUTINE_IMPL.internalName, "continuation", AsmTypes.CONTINUATION.descriptor)

            invokestatic(COROUTINE_MARKER_OWNER, HANDLE_EXCEPTION_ARGUMENT_MARKER_NAME, "()Ljava/lang/Object;", false)

            invokeinterface(AsmTypes.CONTINUATION.internalName, "resumeWithException", "(${AsmTypes.JAVA_THROWABLE_TYPE.descriptor})V")
            //loadSuspendMarker()
            areturn(Type.VOID_TYPE)
        }
    }

    private fun generateDoResume() {
        functionCodegen.generateMethod(
                OtherOrigin(element),
                doResumeDescriptor,
                object : FunctionGenerationStrategy.FunctionDefault(state, element as KtDeclarationWithBody) {
                    override fun doGenerateBody(codegen: ExpressionCodegen, signature: JvmMethodSignature) {
                        codegen.v.visitAnnotation(CONTINUATION_METHOD_ANNOTATION_DESC, true).visitEnd()
                        codegen.initializeCoroutineParameters()
                        super.doGenerateBody(codegen, signature)
                        generateExceptionHandlingBlock(codegen)
                    }
                }
        )
    }


    private fun InstructionAdapter.setLabelValue(value: Int) {
        load(0, AsmTypes.OBJECT_TYPE)
        iconst(value)
        putfield(AsmTypes.COROUTINE_IMPL.internalName, COROUTINE_LABEL_FIELD_NAME, Type.INT_TYPE.descriptor)
    }

    companion object {
        private const val LABEL_VALUE_BEFORE_FIRST_SUSPENSION = 0

        @JvmStatic
        fun create(
                expressionCodegen: ExpressionCodegen,
                originalCoroutineLambdaDescriptor: FunctionDescriptor,
                declaration: KtElement,
                classBuilder: ClassBuilder
        ): ClosureCodegen? {
            if (declaration !is KtFunctionLiteral ) return null
            if (!originalCoroutineLambdaDescriptor.isSuspendLambda) return null

            val descriptorWithContinuationReturnType =
                    createJvmSuspendFunctionView(originalCoroutineLambdaDescriptor)

            val state = expressionCodegen.state
            return CoroutineCodegen(
                    state,
                    declaration,
                    expressionCodegen.context.intoCoroutineClosure(
                            descriptorWithContinuationReturnType, originalCoroutineLambdaDescriptor, expressionCodegen, state.typeMapper
                    ),
                    FunctionGenerationStrategy.FunctionDefault(state, declaration),
                    expressionCodegen.parentCodegen, classBuilder,
                    originalCoroutineLambdaDescriptor
            )
        }
    }
}

private const val COROUTINE_LAMBDA_PARAMETER_PREFIX = "p$"
private const val COROUTINE_VALUE_FIELD_NAME_FOR_INTERCEPT_RESUME = "v$"
private const val COROUTINE_THROWABLE_FIELD_NAME_FOR_INTERCEPT_RESUME = "throwable$"
private const val COROUTINE_VALUE_PARAMETER_SLOT_IN_DO_RESUME = 1
private const val COROUTINE_THROWABLE_PARAMETER_SLOT_IN_DO_RESUME = 2
