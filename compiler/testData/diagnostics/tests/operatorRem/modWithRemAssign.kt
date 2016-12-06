// !DIAGNOSTICS: -UNUSED_PARAMETER

class ModAndRemAssign {
    <!DEPRECATED_BINARY_MOD!>operator<!> fun mod(x: Int) = ModAndRemAssign()
    <!DEPRECATED_BINARY_MOD!>operator<!> fun mod(x: String) = ModAndRemAssign()
    <!DEPRECATED_BINARY_MOD!>operator<!> fun modAssign(x: Int) {}
    operator fun remAssign(x: Int) {}
}

<!DEPRECATED_BINARY_MOD!>operator<!> fun ModAndRemAssign.mod(x: Int) = ModAndRemAssign()
<!DEPRECATED_BINARY_MOD!>operator<!> fun ModAndRemAssign.modAssign(x: Int) {}

fun test() {
    val modAndRemAssign = ModAndRemAssign()
    modAndRemAssign %= 1
}