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

package org.jetbrains.kotlin.resolve

import com.google.common.collect.Lists
import org.jetbrains.kotlin.builtins.KotlinBuiltIns
import org.jetbrains.kotlin.config.LanguageFeature
import org.jetbrains.kotlin.config.LanguageVersionSettings
import org.jetbrains.kotlin.descriptors.FunctionDescriptor
import org.jetbrains.kotlin.descriptors.PropertyDescriptor
import org.jetbrains.kotlin.descriptors.VariableDescriptorWithAccessors
import org.jetbrains.kotlin.diagnostics.Errors.*
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.renderer.DescriptorRenderer
import org.jetbrains.kotlin.resolve.BindingContext.*
import org.jetbrains.kotlin.resolve.calls.callUtil.getCalleeExpressionIfAny
import org.jetbrains.kotlin.resolve.calls.checkers.OperatorCallChecker
import org.jetbrains.kotlin.resolve.calls.inference.ConstraintSystem
import org.jetbrains.kotlin.resolve.calls.inference.ConstraintSystemCompleter
import org.jetbrains.kotlin.resolve.calls.inference.constraintPosition.ConstraintPositionKind.FROM_COMPLETER
import org.jetbrains.kotlin.resolve.calls.inference.toHandle
import org.jetbrains.kotlin.resolve.calls.model.ResolvedCall
import org.jetbrains.kotlin.resolve.calls.results.OverloadResolutionResults
import org.jetbrains.kotlin.resolve.calls.smartcasts.DataFlowInfo
import org.jetbrains.kotlin.resolve.scopes.LexicalScope
import org.jetbrains.kotlin.resolve.scopes.ScopeUtils
import org.jetbrains.kotlin.resolve.scopes.receivers.ExpressionReceiver
import org.jetbrains.kotlin.types.*
import org.jetbrains.kotlin.types.TypeUtils.NO_EXPECTED_TYPE
import org.jetbrains.kotlin.types.TypeUtils.noExpectedType
import org.jetbrains.kotlin.types.checker.KotlinTypeChecker
import org.jetbrains.kotlin.types.expressions.ExpressionTypingContext
import org.jetbrains.kotlin.types.expressions.ExpressionTypingServices
import org.jetbrains.kotlin.types.expressions.ExpressionTypingUtils.createFakeExpressionOfType
import org.jetbrains.kotlin.types.expressions.FakeCallResolver
import org.jetbrains.kotlin.util.OperatorNameConventions

//TODO: check for 'operator' modifier!
class DelegatedPropertyResolver(
        private val builtIns: KotlinBuiltIns,
        private val fakeCallResolver: FakeCallResolver,
        private val expressionTypingServices: ExpressionTypingServices,
        private val languageVersionSettings: LanguageVersionSettings
) {

    fun resolvePropertyDelegate(
            outerDataFlowInfo: DataFlowInfo,
            property: KtProperty,
            variableDescriptor: VariableDescriptorWithAccessors,
            delegateExpression: KtExpression,
            propertyHeaderScope: LexicalScope,
            trace: BindingTrace
    ) {
        property.getter?.let { getter ->
            if (getter.hasBody()) trace.report(ACCESSOR_FOR_DELEGATED_PROPERTY.on(getter))
        }
        property.setter?.let { setter ->
            if (setter.hasBody()) trace.report(ACCESSOR_FOR_DELEGATED_PROPERTY.on(setter))
        }

        val delegateFunctionsScope: LexicalScope
        val initializerScope: LexicalScope

        if (variableDescriptor is PropertyDescriptor) {
            delegateFunctionsScope = ScopeUtils.makeScopeForDelegateConventionFunctions(propertyHeaderScope, variableDescriptor)
            initializerScope = ScopeUtils.makeScopeForPropertyInitializer(propertyHeaderScope, variableDescriptor)
        }
        else {
            initializerScope = propertyHeaderScope
            delegateFunctionsScope = initializerScope
        }

        val byExpressionType = resolveDelegateExpression(delegateExpression, property, variableDescriptor, initializerScope, trace, outerDataFlowInfo)

        resolveToDelegateForMethod(variableDescriptor, delegateExpression, byExpressionType, trace, delegateFunctionsScope, outerDataFlowInfo)
        val delegateType = getResolvedDelegateType(variableDescriptor, delegateExpression, byExpressionType, trace)

        resolveGetValueMethod(variableDescriptor, delegateExpression, delegateType, trace, delegateFunctionsScope, outerDataFlowInfo)
        if (property.isVar) {
            resolveSetValueMethod(variableDescriptor, delegateExpression, delegateType, trace, delegateFunctionsScope, outerDataFlowInfo)
        }

        resolvePropertyDelegatedMethod(variableDescriptor, delegateExpression, delegateType, trace, delegateFunctionsScope, outerDataFlowInfo)
    }

    private fun getResolvedDelegateType(
            variableDescriptor: VariableDescriptorWithAccessors,
            delegateExpression: KtExpression,
            byExpressionType: KotlinType,
            trace: BindingTrace
    ): KotlinType {
        val createDelegateResolvedCall = trace.bindingContext.get(TO_DELEGATE_FOR_RESOLVED_CALL, variableDescriptor)
        if (createDelegateResolvedCall != null) {
            return createDelegateResolvedCall.resultingDescriptor.returnType
                   ?: throw AssertionError("No return type fore 'toDelegateFor' of ${delegateExpression.text}")
        }
        return byExpressionType
    }

    fun getGetValueMethodReturnType(
            variableDescriptor: VariableDescriptorWithAccessors,
            delegateExpression: KtExpression,
            byExpressionType: KotlinType,
            trace: BindingTrace,
            delegateFunctionsScope: LexicalScope,
            dataFlowInfo: DataFlowInfo
    ): KotlinType? {
        resolveToDelegateForMethod(variableDescriptor, delegateExpression, byExpressionType, trace, delegateFunctionsScope, dataFlowInfo)
        val delegateType = getResolvedDelegateType(variableDescriptor, delegateExpression, byExpressionType, trace)
        resolveGetSetValueMethod(variableDescriptor, delegateExpression, delegateType, trace, delegateFunctionsScope, dataFlowInfo, true)

        val resolvedCall = trace.bindingContext.get(DELEGATED_PROPERTY_RESOLVED_CALL, variableDescriptor.getter)
        return if (resolvedCall != null) resolvedCall.resultingDescriptor.returnType else null
    }

    private val isOperatorToDelegateForSupported: Boolean
        get() = languageVersionSettings.supportsFeature(LanguageFeature.OperatorToDelegateFor)

    private fun resolveGetValueMethod(
            variableDescriptor: VariableDescriptorWithAccessors,
            delegateExpression: KtExpression,
            delegateType: KotlinType,
            trace: BindingTrace,
            delegateFunctionsScope: LexicalScope,
            dataFlowInfo: DataFlowInfo
    ) {
        val returnType = getGetValueMethodReturnType(variableDescriptor, delegateExpression, delegateType, trace, delegateFunctionsScope, dataFlowInfo)
        val propertyType = variableDescriptor.type

        /* Do not check return type of get() method of delegate for properties with DeferredType because property type is taken from it */
        if (propertyType !is DeferredType && returnType != null && !KotlinTypeChecker.DEFAULT.isSubtypeOf(returnType, propertyType)) {
            val call = trace.bindingContext.get(DELEGATED_PROPERTY_CALL, variableDescriptor.getter)
                       ?: throw AssertionError("Call should exists for ${variableDescriptor.getter}")
            trace.report(DELEGATE_SPECIAL_FUNCTION_RETURN_TYPE_MISMATCH.on(
                    delegateExpression, renderCall(call, trace.bindingContext), variableDescriptor.type, returnType))
        }
    }

    private fun resolveSetValueMethod(
            variableDescriptor: VariableDescriptorWithAccessors,
            delegateExpression: KtExpression,
            delegateType: KotlinType,
            trace: BindingTrace,
            delegateFunctionsScope: LexicalScope,
            dataFlowInfo: DataFlowInfo
    ) {
        resolveGetSetValueMethod(variableDescriptor, delegateExpression, delegateType, trace,
                                 delegateFunctionsScope, dataFlowInfo, false)
    }

    private fun KtPsiFactory.createExpressionForProperty(): KtExpression {
        return createExpression("null as ${KotlinBuiltIns.FQ_NAMES.kProperty.asSingleFqName().asString()}<*>")
    }

    private fun resolvePropertyDelegatedMethod(
            variableDescriptor: VariableDescriptorWithAccessors,
            delegateExpression: KtExpression,
            delegateType: KotlinType,
            trace: BindingTrace,
            delegateFunctionsScope: LexicalScope,
            dataFlowInfo: DataFlowInfo
    ) {
        val traceToResolvePDMethod = TemporaryBindingTrace.create(trace, "Trace to resolve propertyDelegated method in delegated property")
        val context = ExpressionTypingContext.newContext(traceToResolvePDMethod, delegateFunctionsScope, dataFlowInfo, TypeUtils.NO_EXPECTED_TYPE)

        val psiFactory = KtPsiFactory(delegateExpression)
        val arguments = listOf(psiFactory.createExpressionForProperty())
        val receiver = ExpressionReceiver.create(delegateExpression, delegateType, trace.bindingContext)

        val resolutionResult = fakeCallResolver.makeAndResolveFakeCallInContext(receiver, context, arguments,
                                                                                OperatorNameConventions.PROPERTY_DELEGATED, delegateExpression)

        val call = resolutionResult.first
        val functionResults = resolutionResult.second

        if (!functionResults.isSuccess) {
            val expectedFunction = renderCall(call, traceToResolvePDMethod.bindingContext)
            if (functionResults.isIncomplete || functionResults.isSingleResult ||
                functionResults.resultCode == OverloadResolutionResults.Code.MANY_FAILED_CANDIDATES) {
                trace.report(DELEGATE_PD_METHOD_NONE_APPLICABLE.on(delegateExpression, expectedFunction, functionResults.resultingCalls))
            }
            else if (functionResults.isAmbiguity) {
                trace.report(DELEGATE_SPECIAL_FUNCTION_AMBIGUITY.on(delegateExpression, expectedFunction, functionResults.resultingCalls))
            }
            return
        }

        trace.record(DELEGATED_PROPERTY_PD_RESOLVED_CALL, variableDescriptor, functionResults.resultingCall)
    }

    /* Resolve getValue() or setValue() methods from delegate */
    private fun resolveGetSetValueMethod(
            propertyDescriptor: VariableDescriptorWithAccessors,
            delegateExpression: KtExpression,
            delegateType: KotlinType,
            trace: BindingTrace,
            delegateFunctionsScope: LexicalScope,
            dataFlowInfo: DataFlowInfo,
            isGet: Boolean
    ) {
        val accessor = (if (isGet) propertyDescriptor.getter else propertyDescriptor.setter)
                       ?: throw AssertionError("Delegated property should have getter/setter $propertyDescriptor ${delegateExpression.text}")

        if (trace.bindingContext.get(DELEGATED_PROPERTY_CALL, accessor) != null) return

        val functionResults = getGetSetValueMethod(
                propertyDescriptor, delegateExpression, delegateType, trace, delegateFunctionsScope, dataFlowInfo,
                isGet = isGet, isComplete = true
        )

        if (!functionResults.isSuccess) {
            val call = trace.bindingContext.get(DELEGATED_PROPERTY_CALL, accessor)
                       ?: throw AssertionError("'getDelegatedPropertyConventionMethod' didn't record a call")
            reportDelegateOperatorResolutionError(trace, call, functionResults, delegateExpression, delegateType)
            return
        }

        val resultingDescriptor = functionResults.resultingDescriptor
        val resultingCall = functionResults.resultingCall

        if (!resultingDescriptor.isOperator) {
            val declaration = DescriptorToSourceUtils.descriptorToDeclaration(propertyDescriptor)
            if (declaration is KtProperty) {
                val delegate = declaration.delegate
                if (delegate != null) {
                    val byKeyword = delegate.byKeywordNode.psi
                    OperatorCallChecker.report(byKeyword, resultingDescriptor, trace)
                }
            }
        }

        trace.record(DELEGATED_PROPERTY_RESOLVED_CALL, accessor, resultingCall)
    }

    private fun reportDelegateOperatorResolutionError(
            trace: BindingTrace,
            delegateOperatorCall: Call,
            delegateOperatorResults: OverloadResolutionResults<FunctionDescriptor>,
            delegateExpression: KtExpression,
            delegateType: KotlinType,
            operatorRequired: Boolean = true
    ) {
        val expectedFunction = renderCall(delegateOperatorCall, trace.bindingContext)

        when {
            delegateOperatorResults.isSingleResult ||
            delegateOperatorResults.isIncomplete ||
            delegateOperatorResults.resultCode == OverloadResolutionResults.Code.MANY_FAILED_CANDIDATES ->
                trace.report(DELEGATE_SPECIAL_FUNCTION_NONE_APPLICABLE.on(delegateExpression, expectedFunction, delegateOperatorResults.resultingCalls))
            delegateOperatorResults.isAmbiguity ->
                trace.report(DELEGATE_SPECIAL_FUNCTION_AMBIGUITY.on(delegateExpression, expectedFunction, delegateOperatorResults.resultingCalls))
            else ->
                if (operatorRequired) trace.report(DELEGATE_SPECIAL_FUNCTION_MISSING.on(delegateExpression, expectedFunction, delegateType))
        }
    }

    private fun resolveToDelegateForMethod(
            propertyDescriptor: VariableDescriptorWithAccessors,
            byExpression: KtExpression,
            byExpressionType: KotlinType,
            trace: BindingTrace,
            delegateFunctionsScope: LexicalScope,
            dataFlowInfo: DataFlowInfo
    ) {
        if (!isOperatorToDelegateForSupported) return
        if (trace.bindingContext.get(BindingContext.TO_DELEGATE_FOR_CALL, propertyDescriptor) != null) return

        val toDelegateForResults = getToDelegateForMethod(propertyDescriptor, byExpression, byExpressionType,
                                                          trace, delegateFunctionsScope, dataFlowInfo)
        if (!toDelegateForResults.isSuccess) {
            val call = trace.bindingContext.get(BindingContext.TO_DELEGATE_FOR_CALL, propertyDescriptor)
                       ?: throw AssertionError("'getDelegatedPropertyConventionMethod' didn't record a call")
            reportDelegateOperatorResolutionError(trace, call, toDelegateForResults, byExpression, byExpressionType,
                                                  operatorRequired = false)
            return
        }

        val resultingDescriptor = toDelegateForResults.resultingDescriptor
        if (!resultingDescriptor.isOperator) {
            // TODO resolved 'createDelegate' function, which is not an operator - warning?
            return
        }

        val resultingCall = toDelegateForResults.resultingCall
        trace.record(TO_DELEGATE_FOR_RESOLVED_CALL, propertyDescriptor, resultingCall)
    }

    /* Resolve getValue() or setValue() methods from delegate */
    private fun getGetSetValueMethod(
            propertyDescriptor: VariableDescriptorWithAccessors,
            delegateExpression: KtExpression,
            delegateType: KotlinType,
            trace: BindingTrace,
            delegateFunctionsScope: LexicalScope,
            dataFlowInfo: DataFlowInfo,
            isGet: Boolean,
            isComplete: Boolean
    ): OverloadResolutionResults<FunctionDescriptor> {
        val accessor = (if (isGet) propertyDescriptor.getter else propertyDescriptor.setter)
                       ?: throw AssertionError("Delegated property should have getter/setter $propertyDescriptor ${delegateExpression.text}")

        val expectedType = if (isComplete && isGet && propertyDescriptor.type !is DeferredType)
            propertyDescriptor.type
        else
            TypeUtils.NO_EXPECTED_TYPE

        val context = ExpressionTypingContext.newContext(trace, delegateFunctionsScope, dataFlowInfo, expectedType)

        val hasThis = propertyDescriptor.extensionReceiverParameter != null || propertyDescriptor.dispatchReceiverParameter != null

        val arguments = Lists.newArrayList<KtExpression>()
        val psiFactory = KtPsiFactory(delegateExpression)
        arguments.add(psiFactory.createExpression(if (hasThis) "this" else "null"))
        arguments.add(psiFactory.createExpressionForProperty())

        if (!isGet) {
            val fakeArgument = createFakeExpressionOfType(delegateExpression.project, trace,
                                                          "fakeArgument${arguments.size}",
                                                          propertyDescriptor.type) as KtReferenceExpression
            arguments.add(fakeArgument)
            val valueParameters = accessor.valueParameters
            trace.record(REFERENCE_TARGET, fakeArgument, valueParameters[0])
        }

        val functionName = if (isGet) OperatorNameConventions.GET_VALUE else OperatorNameConventions.SET_VALUE
        val receiver = ExpressionReceiver.create(delegateExpression, delegateType, trace.bindingContext)

        val resolutionResult = fakeCallResolver.makeAndResolveFakeCallInContext(receiver, context, arguments, functionName, delegateExpression)

        trace.record(BindingContext.DELEGATED_PROPERTY_CALL, accessor, resolutionResult.first)
        return resolutionResult.second
    }

    private fun getToDelegateForMethod(
            propertyDescriptor: VariableDescriptorWithAccessors,
            delegateExpression: KtExpression,
            delegateExpressionType: KotlinType,
            trace: BindingTrace,
            delegateFunctionsScope: LexicalScope,
            dataFlowInfo: DataFlowInfo
    ): OverloadResolutionResults<FunctionDescriptor> {
        val expectedType = TypeUtils.NO_EXPECTED_TYPE
        val context = ExpressionTypingContext.newContext(trace, delegateFunctionsScope, dataFlowInfo, expectedType)
        val propertyHasReceiver = propertyDescriptor.extensionReceiverParameter != null ||
                                  propertyDescriptor.dispatchReceiverParameter != null
        val arguments = KtPsiFactory(delegateExpression).run {
            listOf(
                    createExpression(if (propertyHasReceiver) "this" else "null"),
                    createExpressionForProperty()
            )
        }
        val functionName = OperatorNameConventions.TO_DELEGATE_FOR
        val receiver = ExpressionReceiver.create(delegateExpression, delegateExpressionType, trace.bindingContext)

        val (toDelegateForCall, toDelegateForResults) =
                fakeCallResolver.makeAndResolveFakeCallInContext(receiver, context, arguments, functionName, delegateExpression)
        trace.record(BindingContext.TO_DELEGATE_FOR_CALL, propertyDescriptor, toDelegateForCall)

        return toDelegateForResults
    }

    //TODO: diagnostics rendering does not belong here
    private fun renderCall(call: Call, context: BindingContext): String {
        val calleeExpression = call.calleeExpression
                               ?: throw AssertionError("CalleeExpression should exists for fake call of convention method")

        return call.valueArguments.joinToString(
                prefix = "${calleeExpression.text}(",
                postfix = ")",
                separator = ", ",
                transform = { argument ->
                    val type = context.getType(argument.getArgumentExpression()!!)!!
                    DescriptorRenderer.SHORT_NAMES_IN_TYPES.renderType(type)
                }
        )
    }

    fun resolveDelegateExpression(
            delegateExpression: KtExpression,
            property: KtProperty,
            variableDescriptor: VariableDescriptorWithAccessors,
            scopeForDelegate: LexicalScope,
            trace: BindingTrace,
            dataFlowInfo: DataFlowInfo
    ): KotlinType {
        val traceToResolveDelegatedProperty = TemporaryBindingTrace.create(trace, "Trace to resolve delegated property")
        val calleeExpression = delegateExpression.getCalleeExpressionIfAny()
        val completer = createConstraintSystemCompleter(property, variableDescriptor, delegateExpression, scopeForDelegate, trace, dataFlowInfo)

        calleeExpression?.let {
            traceToResolveDelegatedProperty.record(CONSTRAINT_SYSTEM_COMPLETER, it, completer)
        }

        val delegateType = expressionTypingServices.safeGetType(scopeForDelegate, delegateExpression, NO_EXPECTED_TYPE, dataFlowInfo, traceToResolveDelegatedProperty)

        traceToResolveDelegatedProperty.commit({ slice, _ -> slice !== CONSTRAINT_SYSTEM_COMPLETER }, true)

        return delegateType
    }

    private fun createConstraintSystemCompleter(
            property: KtProperty,
            variableDescriptor: VariableDescriptorWithAccessors,
            delegateExpression: KtExpression,
            scopeForDelegate: LexicalScope,
            trace: BindingTrace,
            dataFlowInfo: DataFlowInfo
    ): ConstraintSystemCompleter {
        val delegateFunctionsScope = ScopeUtils.makeScopeForDelegateConventionFunctions(scopeForDelegate, variableDescriptor)
        val expectedType = if (property.typeReference != null) variableDescriptor.type else NO_EXPECTED_TYPE

        return object : ConstraintSystemCompleter {
            override fun completeConstraintSystem(constraintSystem: ConstraintSystem.Builder, resolvedCall: ResolvedCall<*>) {
                val returnType = resolvedCall.candidateDescriptor.returnType ?: return

                val typeVariableSubstitutor = constraintSystem.typeVariableSubstitutors[resolvedCall.call.toHandle()]
                                              ?: throw AssertionError("No substitutor in the system for call: " + resolvedCall.call)

                val traceToResolveConventionMethods = TemporaryBindingTrace.create(trace, "Trace to resolve delegated property convention methods")

                val delegateType = getDelegateType(returnType, constraintSystem, typeVariableSubstitutor, traceToResolveConventionMethods)

                val getValueResults = getGetSetValueMethod(
                        variableDescriptor, delegateExpression, delegateType, traceToResolveConventionMethods, delegateFunctionsScope, dataFlowInfo,
                        isGet = true, isComplete = false
                )
                if (conventionMethodFound(getValueResults)) {
                    val getValueDescriptor = getValueResults.resultingDescriptor
                    val getValueReturnType = getValueDescriptor.returnType
                    if (getValueReturnType != null && !TypeUtils.noExpectedType(expectedType)) {
                        val returnTypeInSystem = typeVariableSubstitutor.substitute(getValueReturnType, Variance.INVARIANT)
                        if (returnTypeInSystem != null) {
                            constraintSystem.addSubtypeConstraint(returnTypeInSystem, expectedType, FROM_COMPLETER.position())
                        }
                    }
                    addConstraintForThisValue(constraintSystem, typeVariableSubstitutor, getValueDescriptor)
                }
                if (!variableDescriptor.isVar) return

                // For the case: 'val v by d' (no declared type).
                // When we add a constraint for 'set' method for delegated expression 'd' we use a type of the declared variable 'v'.
                // But if the type isn't known yet, the constraint shouldn't be added (we try to infer the type of 'v' here as well).
                if (variableDescriptor.returnType is DeferredType) return

                val setValueResults = getGetSetValueMethod(
                        variableDescriptor, delegateExpression, delegateType, traceToResolveConventionMethods, delegateFunctionsScope, dataFlowInfo,
                        isGet = false, isComplete = false
                )
                if (conventionMethodFound(setValueResults)) {
                    val setValueDescriptor = setValueResults.resultingDescriptor
                    val setValueParameters = setValueDescriptor.valueParameters
                    if (setValueParameters.size == 3) {
                        if (!noExpectedType(expectedType)) {
                            val thisParameterType = setValueParameters[2].type
                            val substitutedThisParameterType = typeVariableSubstitutor.substitute(thisParameterType, Variance.INVARIANT)
                            constraintSystem.addSubtypeConstraint(expectedType, substitutedThisParameterType, FROM_COMPLETER.position())
                        }
                        addConstraintForThisValue(constraintSystem, typeVariableSubstitutor, setValueDescriptor)
                    }
                }
            }

            private fun getDelegateType(
                    byExpressionType: KotlinType,
                    constraintSystem: ConstraintSystem.Builder,
                    typeVariableSubstitutor: TypeSubstitutor,
                    traceToResolveConventionMethods: TemporaryBindingTrace
            ): KotlinType {
                if (isOperatorToDelegateForSupported) {
                    val toDelegateForResults = getToDelegateForMethod(
                            variableDescriptor, delegateExpression, byExpressionType,
                            traceToResolveConventionMethods, delegateFunctionsScope, dataFlowInfo
                    )
                    if (conventionMethodFound(toDelegateForResults)) {
                        val toDelegateForDescriptor = toDelegateForResults.resultingDescriptor
                        val toDelegateForReturnType = toDelegateForDescriptor.returnType
                        if (toDelegateForDescriptor.isOperator) {
                            addConstraintForThisValue(constraintSystem, typeVariableSubstitutor, toDelegateForDescriptor)
                            return toDelegateForReturnType
                                   ?: throw AssertionError("No return type fore 'createDelegate' of ${delegateExpression.text}")
                        }
                    }
                }

                return byExpressionType
            }

            private fun conventionMethodFound(results: OverloadResolutionResults<FunctionDescriptor>): Boolean =
                    results.isSuccess ||
                    results.isSingleResult && results.resultCode == OverloadResolutionResults.Code.SINGLE_CANDIDATE_ARGUMENT_MISMATCH

            private fun addConstraintForThisValue(
                    constraintSystem: ConstraintSystem.Builder,
                    typeVariableSubstitutor: TypeSubstitutor,
                    resultingDescriptor: FunctionDescriptor
            ) {
                val extensionReceiver = variableDescriptor.extensionReceiverParameter
                val dispatchReceiver = variableDescriptor.dispatchReceiverParameter
                val typeOfThis = extensionReceiver?.type ?: dispatchReceiver?.type ?: builtIns.nullableNothingType

                val valueParameters = resultingDescriptor.valueParameters
                if (valueParameters.isEmpty()) return
                val valueParameterForThis = valueParameters[0]

                constraintSystem.addSubtypeConstraint(
                        typeOfThis,
                        typeVariableSubstitutor.substitute(valueParameterForThis.type, Variance.INVARIANT),
                        FROM_COMPLETER.position()
                )
            }
        }
    }
}
