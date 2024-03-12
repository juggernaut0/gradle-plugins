package dev.twarner.gradle

import org.gradle.api.DefaultTask
import org.gradle.api.file.ConfigurableFileCollection
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputFiles
import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.tasks.TaskAction
import kotlin.io.path.ExperimentalPathApi
import kotlin.io.path.createDirectories
import kotlin.io.path.deleteRecursively

abstract class GenerateResolverStubsTask : DefaultTask() {
    @get:InputFiles
    abstract val inputSchemas: ConfigurableFileCollection

    @get:Input
    abstract val packageName: Property<String>

    @get:OutputDirectory
    abstract val outputDir: DirectoryProperty

    @OptIn(ExperimentalPathApi::class)
    @TaskAction
    fun generate() {
        val schemaFiles = inputSchemas.files.map { it.toPath() }
        val packageName = packageName.get()
        val outputDirectory = outputDir.get().asFile.toPath()

        outputDirectory.deleteRecursively()

        for (schemaFile in schemaFiles) {
            generateResolverStubs(schemaFile, outputDirectory, packageName)
        }
    }
}
