package dev.twarner.gradle.openapi

import com.squareup.kotlinpoet.*
import com.squareup.kotlinpoet.ParameterizedTypeName.Companion.parameterizedBy
import io.swagger.parser.OpenAPIParser
import io.swagger.v3.oas.models.Operation
import io.swagger.v3.oas.models.media.Schema
import io.swagger.v3.parser.core.models.ParseOptions
import org.gradle.api.DefaultTask
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputFile
import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.tasks.TaskAction

abstract class GenerateApiTask : DefaultTask() {
    @get:InputFile
    abstract val inputSpec: RegularFileProperty

    @get:Input
    abstract val packageName: Property<String>

    @get:OutputDirectory
    abstract val outputDir: DirectoryProperty

    @TaskAction
    fun generate() {
        val packageName = packageName.get()
        val outputDir = outputDir.get().asFile
        outputDir.deleteRecursively()

        val schemaLocation = inputSpec.get().asFile.absolutePath.let { "file://$it" }
        val parseOptions = ParseOptions().apply {
            isResolveFully = true
        }
        val model = OpenAPIParser().readLocation(schemaLocation, null, parseOptions).openAPI

        printSpec(model, logger::info)

        val allSchemas = nameSchemas(model)
        createModelsFile(packageName, allSchemas).writeTo(outputDir)
        createRoutesFile(packageName, model.paths).writeTo(outputDir)
    }
}
