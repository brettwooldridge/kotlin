package kotlin.sequences

internal platform class ConstrainedOnceSequence<T> : Sequence<T> {
    constructor(sequence: Sequence<T>)

    override fun iterator(): Iterator<T>
}
