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

package org.jetbrains.kotlin.idea.quickfix.createImpl

import com.intellij.codeInsight.intention.IntentionAction
import com.intellij.ide.util.PackageUtil
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.module.Module
import com.intellij.openapi.module.ModuleUtilCore
import com.intellij.openapi.project.Project
import com.intellij.openapi.roots.ModuleRootManager
import com.intellij.psi.JavaDirectoryService
import com.intellij.psi.codeStyle.CodeStyleManager
import org.jetbrains.kotlin.builtins.KotlinBuiltIns
import org.jetbrains.kotlin.descriptors.*
import org.jetbrains.kotlin.diagnostics.Diagnostic
import org.jetbrains.kotlin.diagnostics.DiagnosticFactory
import org.jetbrains.kotlin.diagnostics.Errors
import org.jetbrains.kotlin.idea.core.ShortenReferences
import org.jetbrains.kotlin.idea.core.TemplateKind
import org.jetbrains.kotlin.idea.core.getFunctionBodyTextFromTemplate
import org.jetbrains.kotlin.idea.core.toDescriptor
import org.jetbrains.kotlin.idea.project.TargetPlatformDetector
import org.jetbrains.kotlin.idea.quickfix.KotlinQuickFixAction
import org.jetbrains.kotlin.idea.quickfix.KotlinSingleIntentionActionFactory
import org.jetbrains.kotlin.idea.refactoring.getOrCreateKotlinFile
import org.jetbrains.kotlin.idea.util.IdeDescriptorRenderers
import org.jetbrains.kotlin.idea.util.projectStructure.allModules
import org.jetbrains.kotlin.lexer.KtTokens
import org.jetbrains.kotlin.psi.*

sealed class CreateHeaderImplementationFix<out D : KtNamedDeclaration>(
        declaration: D,
        private val implPlatformKind: PlatformKind,
        private val generateIt: KtPsiFactory.(Project, D) -> D?
) : KotlinQuickFixAction<D>(declaration) {

    override fun getFamilyName() = text

    protected abstract val elementType: String

    override fun getText() = "Create header $elementType implementation for platform ${implPlatformKind.name}"

    override final fun invoke(project: Project, editor: Editor?, file: KtFile) {
        val element = element ?: return
        val factory = KtPsiFactory(project)
        val implFile = getOrCreateImplementationFile(project) ?: return

        val implDeclaration = implFile.add(factory.generateIt(project, element) ?: return) as KtElement
        val reformatted = CodeStyleManager.getInstance(project).reformat(implDeclaration)
        ShortenReferences.DEFAULT.process(reformatted as KtElement)
    }

    private fun Project.implementationModuleOf(headerModule: Module) =
            allModules().firstOrNull {
                TargetPlatformDetector.getPlatform(it).kind == implPlatformKind &&
                headerModule in ModuleRootManager.getInstance(it).dependencies
            }

    private fun getOrCreateImplementationFile(
            project: Project
    ): KtFile? {
        val declaration = element as? KtNamedDeclaration ?: return null
        val name = declaration.name ?: return null

        val headerDir = declaration.containingFile.containingDirectory
        val headerPackage = JavaDirectoryService.getInstance().getPackage(headerDir)

        val headerModule = ModuleUtilCore.findModuleForPsiElement(declaration) ?: return null
        val implModule = project.implementationModuleOf(headerModule) ?: return null
        val implDirectory = PackageUtil.findOrCreateDirectoryForPackage(
                implModule, headerPackage?.qualifiedName ?: "", null, false
        ) ?: return null
        return getOrCreateKotlinFile("$name.kt", implDirectory)
    }

    companion object : KotlinSingleIntentionActionFactory() {
        override fun createAction(diagnostic: Diagnostic): IntentionAction? {
            val d = DiagnosticFactory.cast(diagnostic, Errors.HEADER_WITHOUT_IMPLEMENTATION)
            val declaration = d.psiElement as? KtNamedDeclaration ?: return null
            val compatibility = d.c
            if (compatibility.isNotEmpty()) return null
            val implPlatformKind = d.b.platformKind
            return when (declaration) {
                is KtClassOrObject -> CreateHeaderClassImplementationFix(declaration, implPlatformKind)
                is KtFunction -> CreateHeaderFunctionImplementationFix(declaration, implPlatformKind)
                is KtProperty -> CreateHeaderPropertyImplementationFix(declaration, implPlatformKind)
                else -> null
            }
        }
    }
}

class CreateHeaderClassImplementationFix(
        klass: KtClassOrObject,
        implPlatformKind: PlatformKind
) : CreateHeaderImplementationFix<KtClassOrObject>(klass, implPlatformKind, { project, element ->
    generateClassOrObject(project, element, implNeeded = true)
}) {

    override val elementType = if ((element as? KtClass)?.isInterface() ?: false) "interface" else "class"
}

class CreateHeaderPropertyImplementationFix(
        property: KtProperty,
        implPlatformKind: PlatformKind
) : CreateHeaderImplementationFix<KtProperty>(property, implPlatformKind, { project, element ->
    val descriptor = element.toDescriptor() as? PropertyDescriptor
    descriptor?.let { generateProperty(project, element, descriptor, implNeeded = true) }
}) {

    override val elementType = "property"
}

class CreateHeaderFunctionImplementationFix(
        function: KtFunction,
        implPlatformKind: PlatformKind
) : CreateHeaderImplementationFix<KtFunction>(function, implPlatformKind, { project, element ->
    val descriptor = element.toDescriptor() as? FunctionDescriptor
    descriptor?.let { generateFunction(project, element, descriptor, implNeeded = true) }
}) {

    override val elementType = "function"
}

private fun KtModifierListOwner.replaceHeaderModifier(implNeeded: Boolean) {
    if (implNeeded) {
        addModifier(KtTokens.IMPL_KEYWORD)
    }
    else {
        removeModifier(KtTokens.HEADER_KEYWORD)
    }
}

private fun KtPsiFactory.generateClassOrObject(
        project: Project,
        headerClass: KtClassOrObject,
        implNeeded: Boolean
): KtClassOrObject {
    val header = headerClass.text
    val implClass = if (headerClass is KtObjectDeclaration) createObject(header) else createClass(header)
    if (headerClass !is KtClass || !headerClass.isInterface()) {
        implClass.declarations.forEach {
            if (it !is KtEnumEntry &&
                it !is KtClassOrObject &&
                !it.hasModifier(KtTokens.ABSTRACT_KEYWORD)) {
                it.delete()
            }
        }

        declLoop@ for (headerDeclaration in headerClass.declarations) {
            if (headerDeclaration.hasModifier(KtTokens.ABSTRACT_KEYWORD)) continue
            val descriptor = headerDeclaration.toDescriptor() ?: continue
            val implDeclaration: KtDeclaration = when (headerDeclaration) {
                is KtFunction -> generateFunction(project, headerDeclaration, descriptor as FunctionDescriptor, implNeeded = true)
                is KtProperty -> generateProperty(project, headerDeclaration, descriptor as PropertyDescriptor, implNeeded = true)
                else -> continue@declLoop
            }
            implClass.addDeclaration(implDeclaration)
        }
    }

    return implClass.apply {
        replaceHeaderModifier(implNeeded)
    }
}

private fun KtPsiFactory.generateFunction(
        project: Project,
        headerFunction: KtFunction,
        descriptor: FunctionDescriptor,
        implNeeded: Boolean
): KtFunction {
    val returnType = descriptor.returnType
    val body = run {
        if (returnType != null && !KotlinBuiltIns.isUnit(returnType)) {
            val delegation = getFunctionBodyTextFromTemplate(
                    project,
                    TemplateKind.FUNCTION,
                    descriptor.name.asString(),
                    IdeDescriptorRenderers.SOURCE_CODE.renderType(returnType)
            )

            "{$delegation\n}"
        }
        else {
            "{}"
        }
    }

    return if (headerFunction is KtSecondaryConstructor) {
        createSecondaryConstructor(headerFunction.text + " " +  body)
    }
    else {
        createFunction(headerFunction.text + " " +  body).apply {
            replaceHeaderModifier(implNeeded)
            if (returnType != null && KotlinBuiltIns.isUnit(returnType)) {
                typeReference = null
            }
        }
    }
}

private fun KtPsiFactory.generateProperty(
        project: Project,
        headerProperty: KtProperty,
        descriptor: PropertyDescriptor,
        implNeeded: Boolean
): KtProperty {
    val body = buildString {
        append("\nget()")
        append(" = ")
        append(getFunctionBodyTextFromTemplate(
                project,
                TemplateKind.FUNCTION,
                descriptor.name.asString(),
                descriptor.returnType?.let { IdeDescriptorRenderers.SOURCE_CODE.renderType(it) } ?: ""
        ))
        if (descriptor.isVar) {
            append("\nset(value) {}")
        }
    }
    return createProperty(headerProperty.text + " " + body).apply {
        replaceHeaderModifier(implNeeded)
    }
}

