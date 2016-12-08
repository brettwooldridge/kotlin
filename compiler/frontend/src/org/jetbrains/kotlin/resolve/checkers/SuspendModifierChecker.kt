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

package org.jetbrains.kotlin.resolve.checkers

import org.jetbrains.kotlin.builtins.isFunctionType
import org.jetbrains.kotlin.descriptors.DeclarationDescriptor
import org.jetbrains.kotlin.descriptors.FunctionDescriptor
import org.jetbrains.kotlin.diagnostics.DiagnosticSink
import org.jetbrains.kotlin.diagnostics.Errors
import org.jetbrains.kotlin.lexer.KtTokens
import org.jetbrains.kotlin.psi.KtDeclaration
import org.jetbrains.kotlin.psi.KtDeclarationWithBody
import org.jetbrains.kotlin.resolve.BindingContext

object SuspendModifierChecker : SimpleDeclarationChecker {
    override fun check(
            declaration: KtDeclaration,
            descriptor: DeclarationDescriptor,
            diagnosticHolder: DiagnosticSink,
            bindingContext: BindingContext
    ) {
        val functionDescriptor = descriptor as? FunctionDescriptor ?: return
        if (declaration !is KtDeclarationWithBody) return

        for ((parameterDescriptor, parameterDeclaration) in functionDescriptor.valueParameters.zip(declaration.valueParameters)) {
            val suspendModifier = parameterDeclaration.modifierList?.getModifier(KtTokens.SUSPEND_KEYWORD) ?: continue

            fun report(message: String) {
                diagnosticHolder.report(Errors.INAPPLICABLE_MODIFIER.on(suspendModifier, KtTokens.SUSPEND_KEYWORD, message))
            }

            if (!parameterDescriptor.type.isFunctionType) {
                report("parameter should have function type with extension like '() -> T'")
                continue
            }

            if (functionDescriptor.isInline && !parameterDescriptor.isNoinline) {
                report("coroutine parameter of inline function should be marked as 'noinline'")
                continue
            }
        }
    }
}
