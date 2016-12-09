package kotlin

import kotlin.annotation.AnnotationTarget.*

open platform class Error : Throwable {
    constructor(message: String)
}

open platform class Exception : Throwable

open platform class IllegalArgumentException : RuntimeException {
    constructor()
    constructor(message: String)
}

open platform class IllegalStateException : RuntimeException {
    constructor(message: String)
}

open platform class IndexOutOfBoundsException : RuntimeException {
    constructor(message: String)
}

open platform class NoSuchElementException : RuntimeException {
    constructor()
    constructor(message: String)
}

open platform class RuntimeException : Exception {
    constructor()
    constructor(message: String)
}

open platform class UnsupportedOperationException : RuntimeException {
    constructor(message: String)
}

// From kotlin.kt

internal platform fun <T> arrayOfNulls(reference: Array<out T>, size: Int): Array<T>
internal inline platform fun <K, V> Map<K, V>.toSingletonMapOrSelf(): Map<K, V>
internal inline platform fun <K, V> Map<out K, V>.toSingletonMap(): Map<K, V>
internal inline platform fun <T> Array<out T>.copyToArrayOfAny(isVarargs: Boolean): Array<out Any?>

platform interface Serializable

// From concurrent.kt

@Target(PROPERTY, FIELD)
platform annotation class Volatile

inline platform fun <R> synchronized(lock: Any, crossinline block: () -> R): R


// TODO: :(
platform object Math {
    fun max(a: Int, b: Int): Int
    fun min(a: Int, b: Int): Int
}

public platform class Pair<out A, out B>(
        first: A,
        second: B
)
