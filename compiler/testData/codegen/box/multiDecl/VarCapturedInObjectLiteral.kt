// TODO: enable JS backend when issue KT-14535 is fixed
// TARGET_BACKEND: JVM
class A {
    operator fun component1() = 1
    operator fun component2() = 2
}


fun box() : String {
    var (a, b) = A()

    val local = object {
        public fun run() {
            a = 3
        }
    }
    local.run()
    return if (a == 3 && b == 2) "OK" else "fail"
}
