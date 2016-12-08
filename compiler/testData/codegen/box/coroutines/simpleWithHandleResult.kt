// IGNORE_BACKEND: JS

suspend fun suspendHere(): String = suspendWithCurrentContinuation { x ->
    x.resume("OK")
    SuspendMarker
}

fun builder(c: @Suspend() () -> Int): Int {
    var res = 0

    (c as ((Continuation<Int>) -> Int))(object : Continuation<Int> {
        override fun resume(data: Int) {
            res = data
        }

        override fun resumeWithException(exception: Throwable) {
            throw exception
        }
    })

    return res
}



fun box(): String {
    var result = ""

    val handledResult = builder {
        result = suspendHere()
        56
    }

    if (handledResult != 56) return "fail 1: $handledResult"

    return result
}
