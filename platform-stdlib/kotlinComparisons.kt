package kotlin.comparisons

platform interface Comparator<T> {
    fun compare(a: T, b: T): Int
}
