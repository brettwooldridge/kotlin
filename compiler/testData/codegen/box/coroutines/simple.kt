// IGNORE_BACKEND: JS

suspend fun suspendHere(): String = suspendWithCurrentContinuation { x ->
    x.resume("OK")
    SuspendMarker
}

fun builder(c: @Suspend() () -> Unit) {
    (c as ((Continuation<Unit>) -> Unit))(object : Continuation<Unit> {
        override fun resume(data: Unit) {

        }

        override fun resumeWithException(exception: Throwable) {
            throw exception
        }
    })
}

fun box(): String {
    var result = ""

    builder {
        result = suspendHere()
    }

    return result
}
