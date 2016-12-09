package kotlin.collections

public platform class IndexedValue<out T>(index: Int, value: T)


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


// From collections.kt

platform inline fun <reified T> Collection<T>.toTypedArray(): Array<T>

platform fun <T : Comparable<T>> MutableList<T>.sort(): Unit
platform fun <T> MutableList<T>.sortWith(comparator: Comparator<in T>): Unit
