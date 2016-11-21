package kotlin.collections

open platform class ArrayList<E> : MutableList<E> {
    constructor(capacity: Int)
    constructor()
    constructor(c: Collection<E>)

    // From List
    override val size: Int
    override fun isEmpty(): Boolean
    override fun contains(element: @UnsafeVariance E): Boolean
    override fun containsAll(elements: Collection<@UnsafeVariance E>): Boolean
    override operator fun get(index: Int): E
    override fun indexOf(element: @UnsafeVariance E): Int
    override fun lastIndexOf(element: @UnsafeVariance E): Int

    // From MutableCollection
    override fun iterator(): MutableIterator<E>

    // From MutableList
    override fun add(element: E): Boolean
    override fun remove(element: E): Boolean
    override fun addAll(elements: Collection<E>): Boolean
    override fun addAll(index: Int, elements: Collection<E>): Boolean
    override fun removeAll(elements: Collection<E>): Boolean
    override fun retainAll(elements: Collection<E>): Boolean
    override fun clear()
    override operator fun set(index: Int, element: E): E
    override fun add(index: Int, element: E)
    override fun removeAt(index: Int): E
    override fun listIterator(): MutableListIterator<E>
    override fun listIterator(index: Int): MutableListIterator<E>
    override fun subList(fromIndex: Int, toIndex: Int): MutableList<E>
}

open platform class HashMap<K, V> : MutableMap<K, V> {
    constructor(initialCapacity: Int)
    constructor()

    // From Map
    override val size: Int
    override fun isEmpty(): Boolean
    override fun containsKey(key: K): Boolean
    override fun containsValue(value: @UnsafeVariance V): Boolean
    override operator fun get(key: K): V?

    // From MutableMap
    override fun put(key: K, value: V): V?
    override fun remove(key: K): V?
    override fun putAll(from: Map<out K, V>)
    override fun clear()
    override val keys: MutableSet<K>
    override val values: MutableCollection<V>
    override val entries: MutableSet<MutableMap.MutableEntry<K, V>>
}

open platform class LinkedHashMap<K, V> : HashMap<K, V> {
    constructor(initialCapacity: Int)
    constructor()
    constructor(m: Map<out K, V>)
}

open platform class HashSet<E> : MutableSet<E> {
    constructor()
    constructor(initialCapacity: Int)

    // From Set
    override val size: Int
    override fun isEmpty(): Boolean
    override fun contains(element: @UnsafeVariance E): Boolean
    override fun containsAll(elements: Collection<@UnsafeVariance E>): Boolean

    // From MutableSet
    override fun iterator(): MutableIterator<E>
    override fun add(element: E): Boolean
    override fun remove(element: E): Boolean
    override fun addAll(elements: Collection<E>): Boolean
    override fun removeAll(elements: Collection<E>): Boolean
    override fun retainAll(elements: Collection<E>): Boolean
    override fun clear()
}

open platform class LinkedHashSet<E> : HashSet<E> {
    constructor(initialCapacity: Int)
    constructor()
    constructor(c: Collection<E>)
}

platform interface RandomAccess


platform abstract class AbstractMutableList<E> : MutableList<E> {
    protected constructor()

    // From List
    override fun isEmpty(): Boolean
    override fun contains(element: @UnsafeVariance E): Boolean
    override fun containsAll(elements: Collection<@UnsafeVariance E>): Boolean
    override fun indexOf(element: @UnsafeVariance E): Int
    override fun lastIndexOf(element: @UnsafeVariance E): Int

    // From MutableCollection
    override fun iterator(): MutableIterator<E>

    // From MutableList
    override fun add(element: E): Boolean
    override fun remove(element: E): Boolean
    override fun addAll(elements: Collection<E>): Boolean
    override fun addAll(index: Int, elements: Collection<E>): Boolean
    override fun removeAll(elements: Collection<E>): Boolean
    override fun retainAll(elements: Collection<E>): Boolean
    override fun clear()
    override fun listIterator(): MutableListIterator<E>
    override fun listIterator(index: Int): MutableListIterator<E>
    override fun subList(fromIndex: Int, toIndex: Int): MutableList<E>
}

// From kotlin_special.kt

platform fun <T> Array<out T>.asList(): List<T>
platform inline fun BooleanArray.asList(): List<Boolean>
platform inline fun ByteArray.asList(): List<Byte>
platform inline fun CharArray.asList(): List<Char>
platform inline fun DoubleArray.asList(): List<Double>
platform inline fun FloatArray.asList(): List<Float>
platform inline fun IntArray.asList(): List<Int>
platform inline fun LongArray.asList(): List<Long>
platform inline fun ShortArray.asList(): List<Short>

platform inline fun <T> Array<out T>.copyOf(): Array<T>
platform inline fun BooleanArray.copyOf(): BooleanArray
platform inline fun ByteArray.copyOf(): ByteArray
platform inline fun CharArray.copyOf(): CharArray
platform inline fun DoubleArray.copyOf(): DoubleArray
platform inline fun FloatArray.copyOf(): FloatArray
platform inline fun IntArray.copyOf(): IntArray
platform inline fun LongArray.copyOf(): LongArray
platform inline fun ShortArray.copyOf(): ShortArray

platform fun ByteArray.copyOf(newSize: Int): ByteArray
platform fun ShortArray.copyOf(newSize: Int): ShortArray
platform fun IntArray.copyOf(newSize: Int): IntArray
platform fun LongArray.copyOf(newSize: Int): LongArray
platform fun FloatArray.copyOf(newSize: Int): FloatArray
platform fun DoubleArray.copyOf(newSize: Int): DoubleArray
platform fun BooleanArray.copyOf(newSize: Int): BooleanArray
platform fun CharArray.copyOf(newSize: Int): CharArray
platform fun <T> Array<out T>.copyOf(newSize: Int): Array<T?>

platform inline fun <T> Array<out T>.copyOfRange(fromIndex: Int, toIndex: Int): Array<T>
platform inline fun BooleanArray.copyOfRange(fromIndex: Int, toIndex: Int): BooleanArray
platform inline fun ByteArray.copyOfRange(fromIndex: Int, toIndex: Int): ByteArray
platform inline fun CharArray.copyOfRange(fromIndex: Int, toIndex: Int): CharArray
platform inline fun DoubleArray.copyOfRange(fromIndex: Int, toIndex: Int): DoubleArray
platform inline fun FloatArray.copyOfRange(fromIndex: Int, toIndex: Int): FloatArray
platform inline fun IntArray.copyOfRange(fromIndex: Int, toIndex: Int): IntArray
platform inline fun LongArray.copyOfRange(fromIndex: Int, toIndex: Int): LongArray
platform inline fun ShortArray.copyOfRange(fromIndex: Int, toIndex: Int): ShortArray

platform inline operator fun <T> Array<out T>.plus(element: T): Array<T>
platform inline operator fun BooleanArray.plus(element: Boolean): BooleanArray
platform inline operator fun ByteArray.plus(element: Byte): ByteArray
platform inline operator fun CharArray.plus(element: Char): CharArray
platform inline operator fun DoubleArray.plus(element: Double): DoubleArray
platform inline operator fun FloatArray.plus(element: Float): FloatArray
platform inline operator fun IntArray.plus(element: Int): IntArray
platform inline operator fun LongArray.plus(element: Long): LongArray
platform inline operator fun ShortArray.plus(element: Short): ShortArray

platform operator fun <T> Array<out T>.plus(elements: Collection<T>): Array<T>
platform operator fun BooleanArray.plus(elements: Collection<Boolean>): BooleanArray
platform operator fun ByteArray.plus(elements: Collection<Byte>): ByteArray
platform operator fun CharArray.plus(elements: Collection<Char>): CharArray
platform operator fun DoubleArray.plus(elements: Collection<Double>): DoubleArray
platform operator fun FloatArray.plus(elements: Collection<Float>): FloatArray
platform operator fun IntArray.plus(elements: Collection<Int>): IntArray
platform operator fun LongArray.plus(elements: Collection<Long>): LongArray
platform operator fun ShortArray.plus(elements: Collection<Short>): ShortArray

platform inline operator fun <T> Array<out T>.plus(elements: Array<out T>): Array<T>
platform inline operator fun BooleanArray.plus(elements: BooleanArray): BooleanArray
platform inline operator fun ByteArray.plus(elements: ByteArray): ByteArray
platform inline operator fun CharArray.plus(elements: CharArray): CharArray
platform inline operator fun DoubleArray.plus(elements: DoubleArray): DoubleArray
platform inline operator fun FloatArray.plus(elements: FloatArray): FloatArray
platform inline operator fun IntArray.plus(elements: IntArray): IntArray
platform inline operator fun LongArray.plus(elements: LongArray): LongArray
platform inline operator fun ShortArray.plus(elements: ShortArray): ShortArray

platform inline fun <T> Array<out T>.plusElement(element: T): Array<T>

platform fun ByteArray.sort(): Unit
platform fun CharArray.sort(): Unit
platform fun DoubleArray.sort(): Unit
platform fun FloatArray.sort(): Unit
platform fun IntArray.sort(): Unit
platform fun ShortArray.sort(): Unit
platform fun <T: Comparable<T>> Array<out T>.sort(): Unit
platform fun LongArray.sort(): Unit

platform fun <T> Array<out T>.sort(comparison: (T, T) -> Int): Unit
platform fun ByteArray.sort(comparison: (Byte, Byte) -> Int): Unit
platform fun CharArray.sort(comparison: (Char, Char) -> Int): Unit
platform fun DoubleArray.sort(comparison: (Double, Double) -> Int): Unit
platform fun FloatArray.sort(comparison: (Float, Float) -> Int): Unit
platform fun IntArray.sort(comparison: (Int, Int) -> Int): Unit
platform fun LongArray.sort(comparison: (Long, Long) -> Int): Unit
platform fun ShortArray.sort(comparison: (Short, Short) -> Int): Unit

platform fun <T> Array<out T>.sortWith(comparator: Comparator<in T>): Unit

platform fun BooleanArray.toTypedArray(): Array<Boolean>
platform fun ByteArray.toTypedArray(): Array<Byte>
platform fun CharArray.toTypedArray(): Array<Char>
platform fun DoubleArray.toTypedArray(): Array<Double>
platform fun FloatArray.toTypedArray(): Array<Float>
platform fun IntArray.toTypedArray(): Array<Int>
platform fun LongArray.toTypedArray(): Array<Long>
platform fun ShortArray.toTypedArray(): Array<Short>

// From collections.kt

platform inline fun <reified T> Collection<T>.toTypedArray(): Array<T>

platform fun <T : Comparable<T>> MutableList<T>.sort(): Unit
platform fun <T> MutableList<T>.sortWith(comparator: Comparator<in T>): Unit
platform fun <T> MutableList<T>.reverse(): Unit

