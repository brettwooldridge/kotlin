package kotlin.jvm

import kotlin.annotation.AnnotationTarget.*

@Target(FUNCTION, PROPERTY_GETTER, PROPERTY_SETTER, FILE)
platform annotation class JvmName(val name: String)

@Target(FILE)
platform annotation class JvmMultifileClass

platform annotation class JvmField
