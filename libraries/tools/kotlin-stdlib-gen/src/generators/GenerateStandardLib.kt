package generators

import java.io.*
import templates.*
import templates.Family.*

private val COMMON_AUTOGENERATED_WARNING: String = """//
// NOTE THIS FILE IS AUTO-GENERATED by the GenerateStandardLib.kt
// See: https://github.com/JetBrains/kotlin/tree/master/libraries/stdlib
//"""

/**
 * Generates methods in the standard library which are mostly identical
 * but just using a different input kind.
 *
 * Kinda like mimicking source macros here, but this avoids the inefficiency of type conversions
 * at runtime.
 */
fun main(args: Array<String>) {
    require(args.size == 1) { "Expecting Kotlin project home path as an argument" }
    val baseDir = File(args.first())

    val outDir = baseDir.resolve("libraries/stdlib/src/generated")
    require(outDir.exists()) { "$outDir doesn't exist!" }

    val jsCoreDir = baseDir.resolve("js/js.libraries/src/core/generated")
    require(jsCoreDir.exists()) { "$jsCoreDir doesn't exist!" }

    generateCollectionsAPI(outDir)
    generateCollectionsJsAPI(jsCoreDir)
    generateCommonAPI(baseDir.resolve("out/src/stdlib/common").apply { mkdirs() })

}

val commonGenerators = sequenceOf(
        ::elements,
        ::filtering,
        ::ordering,
        ::arrays,
        ::snapshots,
        ::mapping,
        ::sets,
        ::aggregates,
        ::guards,
        ::generators,
        ::strings,
        ::sequences,
        ::ranges,
        ::numeric,
        ::comparables
)

fun generateCollectionsAPI(outDir: File) {

    val templates = (commonGenerators + ::specialJVM).flatMap { it().sortedBy { it.signature }.asSequence() }

    templates.groupByFileAndWrite(outDir, Platform.JVM)
}

fun generateCollectionsJsAPI(outDir: File) {
    (commonGenerators + ::specialJS).flatMap { it().sortedBy { it.signature }.asSequence() }
            .groupByFileAndWrite(outDir, Platform.JS, { "_${it.name.capitalize()}Js.kt"})
}

fun generateCommonAPI(outDir: File) {
    (commonGenerators + ::specialJVM + ::specialJS).flatMap { it().sortedBy { it.signature }.asSequence() }
            .groupByFileAndWrite(outDir, platform = Platform.Common)
}




private fun Sequence<GenericFunction>.groupByFileAndWrite(
        outDir: File,
        platform: Platform,
        fileNameBuilder: (SourceFile) -> String = { "_${it.name.capitalize()}.kt" }
) {
    val groupedConcreteFunctions =
            map { it.instantiate(platform) }.flatten()
            .groupBy { it.sourceFile }
            // TODO: Change sort order here
            // .mapValues { it.value.sortedBy { it.signature } }

    for ((sourceFile, functions) in groupedConcreteFunctions) {
        val file = outDir.resolve(fileNameBuilder(sourceFile))
        functions.writeTo(file, sourceFile, platform)
    }
}

private fun List<ConcreteFunction>.writeTo(file: File, sourceFile: SourceFile, platform: Platform?) {
    println("Generating file: $file")

    FileWriter(file).use { writer ->
        if (sourceFile.multifile) {
            writer.appendln("@file:kotlin.jvm.JvmMultifileClass")
        }

        writer.appendln("@file:kotlin.jvm.JvmName(\"${sourceFile.jvmClassName}\")")
        if (platform == Platform.JVM)
            writer.appendln("@file:kotlin.jvm.JvmVersion")
        writer.appendln()

        writer.append("package ${sourceFile.packageName ?: "kotlin"}\n\n")
        writer.append("$COMMON_AUTOGENERATED_WARNING\n\n")
        writer.append("import kotlin.comparisons.*\n\n")

        for (f in this) {
            f.textBuilder(writer)
        }
    }
}
