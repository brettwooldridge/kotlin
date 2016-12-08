// IGNORE_BACKEND: JS
// WITH_RUNTIME
// FULL_JDK

fun box(): String {
    val x = gen().joinToString()
    if (x != "1, 2") return "fail1: $x"

    val y = gen().joinToString()
    if (y != "-1") return "fail2: $y"
    return "OK"
}

var was = false

fun gen() = generate<Int> {
    if (was) {
        yield(-1)
        return@generate
    }
    for (i in 1..2) {
        yield(i)
    }
    was = true
}

// LIBRARY CODE

interface Generator<T> {
    suspend fun yield(value: T)
}

fun <T> generate(block: @Suspend() (Generator<T>.() -> Unit)): Sequence<T> = GeneratedSequence(block)

class GeneratedSequence<T>(private val block: @Suspend() (Generator<T>.() -> Unit)) : Sequence<T> {
    override fun iterator(): Iterator<T> = GeneratedIterator(block)
}

class GeneratedIterator<T>( block: @Suspend() (Generator<T>.() -> Unit)) : Iterator<T>, Generator<T> {
    var nextStep: Continuation<Unit>? = null
    var lastValue: T? = null

    init {
        (block as (Generator<T>, Continuation<Unit>) -> Any?).invoke(this, object : Continuation<Unit> {
            override fun resume(data: Unit) {
                nextStep = null
            }

            override fun resumeWithException(exception: Throwable) {
                throw exception
            }
        })
    }

    override suspend fun yield(value: T) = suspendWithCurrentContinuation { c: Continuation<Unit> ->
        nextStep = c
        lastValue = value

        SuspendMarker
    }

    override fun hasNext(): Boolean = nextStep != null

    override fun next(): T {
        val result = lastValue as T
        nextStep!!.resume(Unit)
        return result
    }
}
