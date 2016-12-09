package kotlin.sequences

public platform interface Sequence<out T> {
    /**
     * Returns an [Iterator] that returns the values from the sequence.
     *
     * Throws an exception if the sequence is constrained to be iterated once and `iterator` is invoked the second time.
     */
    public operator fun iterator(): Iterator<T>
}


internal platform class ConstrainedOnceSequence<T> : Sequence<T> {
    constructor(sequence: Sequence<T>)

    override fun iterator(): Iterator<T>
}
