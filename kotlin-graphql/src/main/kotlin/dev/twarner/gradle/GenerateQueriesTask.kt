package dev.twarner.gradle

import org.gradle.api.DefaultTask
import org.gradle.api.file.ConfigurableFileCollection
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputFiles
import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.tasks.TaskAction
import org.gradle.internal.exceptions.MultiCauseException
import kotlin.io.path.bufferedReader
import kotlin.io.path.nameWithoutExtension
import kotlin.io.path.reader

abstract class GenerateQueriesTask : DefaultTask() {
    @get:InputFiles
    abstract val inputQueries: ConfigurableFileCollection

    @get:InputFiles
    abstract val validationSchemas: ConfigurableFileCollection

    @get:Input
    abstract val packageName: Property<String>

    @get:OutputDirectory
    abstract val outputDir: DirectoryProperty

    @TaskAction
    fun generate() {
        val queryFiles = inputQueries.files.map { it.toPath() }
        val schemaFiles = validationSchemas.files.associate { it.nameWithoutExtension to it.toPath() }
        val packageName = packageName.get()
        val outputDirectory = outputDir.get().asFile

        outputDirectory.deleteRecursively()

        for (queryFile in queryFiles) {
            val firstLine = queryFile.bufferedReader().readLine() ?: continue
            if (firstLine.startsWith("# schema ")) {
                val schemaName = firstLine.removePrefix("# schema ")
                val schemaFile = schemaFiles[schemaName] ?: error("No schema file found for $schemaName")
                generateQuery(queryFile, schemaFile, outputDirectory.toPath(), packageName)
            } else {
                val parentExc = RuntimeException("No schema file found to validate query file $queryFile")
                var success = false
                for (schemaFile in schemaFiles.values) {
                    try {
                        generateQuery(queryFile, schemaFile, outputDirectory.toPath(), packageName)
                        success = true
                        break
                    } catch (e: Exception) {
                        parentExc.addSuppressed(e)
                    }
                }
                if (!success) {
                    throw parentExc
                }
            }
        }
    }
}