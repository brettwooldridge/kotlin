package kotlin.text

platform interface Appendable {
    fun append(c: Char): Appendable
}

platform class StringBuilder : Appendable, CharSequence {
    constructor()
    constructor(capacity: Int)
    constructor(seq: CharSequence)

    override val length: Int
    override operator fun get(index: Int): Char
    override fun subSequence(startIndex: Int, endIndex: Int): CharSequence

    fun reverse(): StringBuilder
    override fun append(c: Char): Appendable
}

platform class Regex {
    constructor(pattern: String)
    constructor(pattern: String, option: RegexOption)
    constructor(pattern: String, options: Set<RegexOption>)

    fun matches(input: CharSequence): Boolean
    fun containsMatchIn(input: CharSequence): Boolean
    fun replace(input: CharSequence, replacement: String): String
    fun replace(input: CharSequence, transform: (MatchResult) -> CharSequence): String
    fun replaceFirst(input: CharSequence, replacement: String): String
    fun split(input: CharSequence, limit: Int): List<String>
}

platform class MatchGroup

platform enum class RegexOption

public platform interface MatchGroupCollection : Collection<MatchGroup?> {
    public operator fun get(index: Int): MatchGroup?
}

public platform interface MatchNamedGroupCollection : MatchGroupCollection {
    public operator fun get(name: String): MatchGroup?
}


public platform interface MatchResult {
    public val range: IntRange
    public val value: String
    public val groups: MatchGroupCollection
    public val groupValues: List<String>
    //public val destructured: Destructured

    public fun next(): MatchResult?


}

// From char.kt

platform fun Char.isWhitespace(): Boolean
platform fun Char.toLowerCase(): Char
platform fun Char.toUpperCase(): Char
platform fun Char.isHighSurrogate(): Boolean
platform fun Char.isLowSurrogate(): Boolean

// From string.kt

internal platform fun String.nativeIndexOf(str: String, fromIndex: Int): Int
internal platform fun String.nativeLastIndexOf(str: String, fromIndex: Int): Int
internal platform fun String.nativeStartsWith(s: String, position: Int): Boolean
internal platform fun String.nativeEndsWith(s: String): Boolean

// From stringsCode.kt

internal inline platform fun String.nativeIndexOf(ch: Char, fromIndex: Int): Int
internal inline platform fun String.nativeLastIndexOf(ch: Char, fromIndex: Int): Int

platform fun CharSequence.isBlank(): Boolean
platform fun CharSequence.regionMatches(thisOffset: Int, other: CharSequence, otherOffset: Int, length: Int, ignoreCase: Boolean): Boolean
