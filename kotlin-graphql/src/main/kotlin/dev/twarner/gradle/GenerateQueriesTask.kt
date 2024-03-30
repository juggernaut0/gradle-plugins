package dev.twarner.gradle

import org.gradle.api.DefaultTask
import org.gradle.api.file.ConfigurableFileCollection
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputFiles
import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.tasks.TaskAction
import java.nio.file.Path
import kotlin.io.path.bufferedReader

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

        var generationFailed = false

        for (queryFile in queryFiles) {
            val firstLine = queryFile.bufferedReader().readLine() ?: continue
            if (firstLine.startsWith("# schema ")) {
                val schemaName = firstLine.removePrefix("# schema ")
                val schemaFile = schemaFiles[schemaName] ?: error("No schema file found for $schemaName")
                try {
                    generateQuery(queryFile, schemaFile, outputDirectory.toPath(), packageName)
                } catch (e: Exception) {
                    logGenerationError(queryFile, e)
                    generationFailed = true
                }
            } else {
                val parentExc = RuntimeException("No schema file found to validate query file $queryFile")
                var validationSuccess = false
                for (schemaFile in schemaFiles.values) {
                    try {
                        generateQuery(queryFile, schemaFile, outputDirectory.toPath(), packageName)
                        validationSuccess = true
                        break
                    } catch (e: QueryValidationException) {
                        parentExc.addSuppressed(e)
                    } catch (e: Exception) {
                        logGenerationError(queryFile, e)
                        generationFailed = true
                        validationSuccess = true
                        break
                    }
                }
                if (!validationSuccess) {
                    throw parentExc
                }
            }
        }

        if (generationFailed) {
            error("Query generation failed. Check logs for errors")
        }
    }

    private fun logGenerationError(file: Path, e: Exception) {
        logger.quiet("e: $file: ${e.message}")
    }
}
