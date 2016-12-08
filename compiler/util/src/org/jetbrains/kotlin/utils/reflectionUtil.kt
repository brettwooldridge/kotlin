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

package org.jetbrains.kotlin.utils

import org.jetbrains.kotlin.utils.addToStdlib.check
import kotlin.reflect.KCallable
import kotlin.reflect.KClass
import kotlin.reflect.KParameter
import kotlin.reflect.KType
import kotlin.reflect.jvm.jvmErasure

fun tryConstructClassFromStringArgs(scriptClass: Class<*>, scriptArgs: List<String>): Any? {

    try {
        return scriptClass.getConstructor(Array<String>::class.java).newInstance(scriptArgs.toTypedArray())
    }
    catch (e: NoSuchMethodException) {
        for (ctor in scriptClass.kotlin.constructors) {
            val mapping = tryCreateCallableMapping(ctor, scriptArgs.map { NamedArgument(null, it) }.iterator(), StringArgsConverter())
            if (mapping != null) {
                try {
                    return ctor.callBy(mapping)
                }
                catch (e: Exception) { // TODO: find the exact exception type thrown then callBy fails
                }
            }
        }
    }
    return null
}

fun tryCreateCallableMapping(callable: KCallable<*>, args: List<Any?>): Map<KParameter, Any?>? =
        tryCreateCallableMapping(callable, args.map { NamedArgument(null, it) }.iterator(), AnyArgsConverter())

fun tryCreateCallableMappingFromNamedArgs(callable: KCallable<*>, args: List<Pair<String?, Any?>>): Map<KParameter, Any?>? =
        tryCreateCallableMapping(callable, args.map { NamedArgument(it.first, it.second) }.iterator(), AnyArgsConverter())

// ------------------------------------------------

private data class NamedArgument<out T>(val name: String?, val value: T?)

private interface ArgsConverter<T> {

    sealed class Result {
        object Failure : Result()
        class Success(val v: Any?) : Result()
    }
    fun tryConvertSingle(parameter: KParameter, arg: NamedArgument<T>): Result
    fun tryConvertVararg(parameter: KParameter, firstArg: NamedArgument<T>, restArgsIt: Iterator<NamedArgument<T>>): Result
    fun tryConvertTail(parameter: KParameter, firstArg: NamedArgument<T>, restArgsIt: Iterator<NamedArgument<T>>): Result
}

private enum class ArgsTraversalState { UNNAMED, NAMED, TAIL }

private fun <T> tryCreateCallableMapping(callable: KCallable<*>, args: Iterator<NamedArgument<T>>, converter: ArgsConverter<T>): Map<KParameter, Any?>? {
    val res = mutableMapOf<KParameter, Any?>()
    var state = ArgsTraversalState.UNNAMED
    val unboundParams = callable.parameters.toMutableList()
    val argIt = args.iterator()
    while (argIt.hasNext()) {
        if (unboundParams.isEmpty()) return null // failed to match: no param left for the arg
        val arg = argIt.next()
        when {
            state == ArgsTraversalState.UNNAMED && arg.name != null -> state = ArgsTraversalState.NAMED
            state == ArgsTraversalState.NAMED && arg.name == null -> state == ArgsTraversalState.TAIL
            state == ArgsTraversalState.TAIL && arg.name != null -> throw IllegalArgumentException("Illegal mix of named and unnamed arguments")
        }
        // TODO: check the logic of named/unnamed/tail(vararg or lambda) arguments matching
        when (state) {
            ArgsTraversalState.UNNAMED -> {
                val par = unboundParams.removeAt(0)
                // try single argument first
                val cvtRes = converter.tryConvertSingle(par, arg)
                if (cvtRes is ArgsConverter.Result.Success) {
                    if (cvtRes.v == null && !par.type.isMarkedNullable) { // TODO: this is not precise check - see comments to the property; consider better approach
                        // or if we do not allow to overload on nullability, drop this check
                        return null // failed to match: null for a non-nullable value
                    }
                    res.put(par, cvtRes.v)
                }
                else if ((par.type.classifier as? KClass<*>)?.qualifiedName == Array<Any>::class.qualifiedName) {
                    // try vararg
                    val cvtVRes = converter.tryConvertVararg(par, arg, argIt)
                    if (cvtVRes is ArgsConverter.Result.Success) {
                        res.put(par, cvtVRes.v)
                    }
                    else return null // failed to match: no suitable param for unnamed arg
                }
                else return null // failed to match: no suitable param for unnamed arg
            }
            ArgsTraversalState.NAMED -> {
                assert(arg.name != null)
                val parIdx = unboundParams.indexOfFirst { it.name != null && it.name == arg.name }.check { it >= 0 }
                    ?: return null // failed to match: no matching named parameter found
                val par = unboundParams.removeAt(parIdx)
                val cvtRes = converter.tryConvertSingle(par, arg)
                if (cvtRes is ArgsConverter.Result.Success) {
                    res.put(par, cvtRes.v)
                }
                else return null // failed to match: cannot convert arg to param's type
            }
            ArgsTraversalState.TAIL -> {
                assert(arg.name == null)
                val par = unboundParams.removeAt(unboundParams.lastIndex)
                val cvtVRes = converter.tryConvertTail(par, arg, argIt)
                if (cvtVRes is ArgsConverter.Result.Success) {
                    if (argIt.hasNext()) return null // failed to match: not all tail args are consumed
                    res.put(par, cvtVRes.v)
                }
                else return null // failed to match: no suitable param for tail arg(s)
            }
        }
    }
    return when {
        unboundParams.any { !it.isOptional } -> null // fail to match: non-optional params remained
        else -> res
    }
}


private class StringArgsConverter : ArgsConverter<String> {

    override fun tryConvertSingle(parameter: KParameter, arg: NamedArgument<String>): ArgsConverter.Result {

        fun convertPrimitive(type: KType?, arg: String): Any? =
                when (type?.classifier) {
                    String::class -> arg
                    Int::class -> arg.toInt()
                    Long::class -> arg.toLong()
                    Short::class -> arg.toShort()
                    Byte::class -> arg.toByte()
                    Char::class -> arg[0]
                    Float::class -> arg.toFloat()
                    Double::class -> arg.toDouble()
                    Boolean::class -> arg.toBoolean()
                    else -> null
                }

        try {
            if (arg.value != null) {
                val primArgCandidate = convertPrimitive(parameter.type, arg.value)
                if (primArgCandidate != null)
                    return ArgsConverter.Result.Success(primArgCandidate)
            }
            else return ArgsConverter.Result.Success(null)
        }
        catch (e: NumberFormatException) {}

        return ArgsConverter.Result.Failure
    }

    override fun tryConvertVararg(parameter: KParameter, firstArg: NamedArgument<String>, restArgsIt: Iterator<NamedArgument<String>>): ArgsConverter.Result {

        fun convertArray(type: KType?, args: Sequence<String?>): Any? =
                when (type?.classifier) {
                    String::class -> args.toList().toTypedArray()
                    Int::class -> args.map { it?.toInt() }.toList().toTypedArray()
                    Long::class -> args.map { it?.toLong() }.toList().toTypedArray()
                    Short::class -> args.map { it?.toShort() }.toList().toTypedArray()
                    Byte::class -> args.map { it?.toByte() }.toList().toTypedArray()
                    Char::class -> args.map { it?.get(0) }.toList().toTypedArray()
                    Float::class -> args.map { it?.toFloat() }.toList().toTypedArray()
                    Double::class -> args.map { it?.toDouble() }.toList().toTypedArray()
                    Boolean::class -> args.map { it?.toBoolean() }.toList().toTypedArray()
                    else -> null
                }

        try {
            if ((parameter.type.classifier as? KClass<*>)?.qualifiedName == Array<Any>::class.qualifiedName) {
                val arrCompType = parameter.type.arguments.getOrNull(0)?.type
                val arrayArgCandidate = convertArray(arrCompType, sequenceOf(firstArg.value) + restArgsIt.asSequence().map { it.value })
                if (arrayArgCandidate != null)
                    return ArgsConverter.Result.Success(arrayArgCandidate)
            }
        }
        catch (e: NumberFormatException) {}

        return ArgsConverter.Result.Failure
    }

    override fun tryConvertTail(parameter: KParameter, firstArg: NamedArgument<String>, restArgsIt: Iterator<NamedArgument<String>>): ArgsConverter.Result =
            tryConvertVararg(parameter, firstArg, restArgsIt)
}

private class AnyArgsConverter : ArgsConverter<Any> {
    override fun tryConvertSingle(parameter: KParameter, arg: NamedArgument<Any>): ArgsConverter.Result {

        fun convertSingle(type: KType, arg: Any?): Any? = when {
            type.classifier == arg?.javaClass?.kotlin -> arg
            type.jvmErasure.java.isAssignableFrom(arg?.javaClass) -> arg
            else -> null
        }

        val primArgCandidate = convertSingle(parameter.type, arg.value)
        if (primArgCandidate != null)
            return ArgsConverter.Result.Success(primArgCandidate)
        return ArgsConverter.Result.Failure
    }

    override fun tryConvertVararg(parameter: KParameter, firstArg: NamedArgument<Any>, restArgsIt: Iterator<NamedArgument<Any>>): ArgsConverter.Result =
            ArgsConverter.Result.Failure

    override fun tryConvertTail(parameter: KParameter, firstArg: NamedArgument<Any>, restArgsIt: Iterator<NamedArgument<Any>>): ArgsConverter.Result =
            tryConvertSingle(parameter, firstArg)
}

