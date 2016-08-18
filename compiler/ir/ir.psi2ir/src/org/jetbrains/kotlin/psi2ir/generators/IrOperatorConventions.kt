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

package org.jetbrains.kotlin.psi2ir.generators

import com.intellij.psi.tree.IElementType
import org.jetbrains.kotlin.ir.expressions.IrBinaryOperator
import org.jetbrains.kotlin.ir.expressions.IrOperator
import org.jetbrains.kotlin.lexer.KtTokens


fun getIrBinaryOperator(ktOperator: IElementType): IrBinaryOperator? =
        KT_TOKEN_TO_IR_BINARY_OPERATOR[ktOperator]

fun getIrPrefixOperator(ktOperator: IElementType): IrOperator? =
        KT_TOKEN_TO_IR_PREFIX_OPERATOR[ktOperator]

fun getIrPostfixOperator(ktOperator: IElementType): IrOperator? =
        KT_TOKEN_TO_IR_POSTFIX_OPERATOR[ktOperator]

private val KT_TOKEN_TO_IR_BINARY_OPERATOR = mapOf<IElementType, IrBinaryOperator>(
        KtTokens.EQ to IrOperator.EQ,

        KtTokens.PLUSEQ to IrOperator.PLUSEQ,
        KtTokens.MINUSEQ to IrOperator.MINUSEQ,
        KtTokens.MULTEQ to IrOperator.MULTEQ,
        KtTokens.DIVEQ to IrOperator.DIVEQ,
        KtTokens.PERCEQ to IrOperator.PERCEQ,

        KtTokens.PLUS to IrOperator.PLUS,
        KtTokens.MINUS to IrOperator.MINUS,
        KtTokens.MUL to IrOperator.MUL,
        KtTokens.DIV to IrOperator.DIV,
        KtTokens.PERC to IrOperator.PERC,
        KtTokens.RANGE to IrOperator.RANGE,

        KtTokens.LT to IrOperator.LT,
        KtTokens.LTEQ to IrOperator.LTEQ,
        KtTokens.GT to IrOperator.GT,
        KtTokens.GTEQ to IrOperator.GTEQ,

        KtTokens.EQEQ to IrOperator.EQEQ,
        KtTokens.EXCLEQ to IrOperator.EXCLEQ,

        KtTokens.EQEQEQ to IrOperator.EQEQEQ,
        KtTokens.EXCLEQEQEQ to IrOperator.EXCLEQEQ,

        KtTokens.IN_KEYWORD to IrOperator.IN,
        KtTokens.NOT_IN to IrOperator.NOT_IN,

        KtTokens.ANDAND to IrOperator.ANDAND,
        KtTokens.OROR to IrOperator.OROR,

        KtTokens.ELVIS to IrOperator.ELVIS
)

private val KT_TOKEN_TO_IR_PREFIX_OPERATOR = mapOf<IElementType, IrOperator>(
        KtTokens.PLUSPLUS to IrOperator.PREFIX_INCR,
        KtTokens.MINUSMINUS to IrOperator.PREFIX_DECR,
        KtTokens.EXCL to IrOperator.EXCL,
        KtTokens.MINUS to IrOperator.UMINUS
)

private val KT_TOKEN_TO_IR_POSTFIX_OPERATOR = mapOf<IElementType, IrOperator>(
        KtTokens.PLUSPLUS to IrOperator.POSTFIX_INCR,
        KtTokens.MINUSMINUS to IrOperator.POSTFIX_DECR,
        KtTokens.EXCLEXCL to IrOperator.EXCLEXCL
)

val AUGMENTED_ASSIGNMENTS =
        setOf(IrOperator.PLUSEQ, IrOperator.MINUSEQ, IrOperator.MULTEQ, IrOperator.DIVEQ, IrOperator.PERCEQ)

val BINARY_OPERATORS_DESUGARED_TO_CALLS =
        setOf(IrOperator.PLUS, IrOperator.MINUS, IrOperator.MUL, IrOperator.DIV, IrOperator.PERC, IrOperator.RANGE)

val COMPARISON_OPERATORS =
        setOf(IrOperator.LT, IrOperator.LTEQ, IrOperator.GT, IrOperator.GTEQ)

val EQUALITY_OPERATORS =
        setOf(IrOperator.EQEQ, IrOperator.EXCLEQ)

val IDENTITY_OPERATORS =
        setOf(IrOperator.EQEQEQ, IrOperator.EXCLEQEQ)

val IN_OPERATORS =
        setOf(IrOperator.IN, IrOperator.NOT_IN)

val BINARY_BOOLEAN_OPERATORS =
        setOf(IrOperator.ANDAND, IrOperator.OROR)

val PREFIX_INCREMENT_DECREMENT_OPERATORS =
        setOf(IrOperator.PREFIX_INCR, IrOperator.PREFIX_DECR)

val POSTFIX_INCREMENT_DECREMENT_OPERATORS =
        setOf(IrOperator.POSTFIX_INCR, IrOperator.POSTFIX_DECR)