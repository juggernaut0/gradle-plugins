package dev.twarner.gradle

import org.gradle.api.DefaultTask
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.tasks.TaskAction

abstract class GenerateMigrationHelperTask : DefaultTask() {
    @get:Input
    abstract val packageName: Property<String>

    @get:OutputDirectory
    abstract val outputDir: DirectoryProperty

    @get:Internal
    abstract val migratorMainCLass: Property<String>

    init {
        migratorMainCLass.set(packageName.map { "$it.DbMigration" })
    }

    @TaskAction
    fun generate() {
        outputDir.get().asFile.deleteRecursively()

        val packageName = packageName.get()
        val packagePath = packageName.replace(".", "/")
        val destDir = outputDir.get().asFile.resolve(packagePath)
        destDir.mkdirs()
        val destFile = destDir.resolve("DbMigration.java")

        val contents = GenerateMigrationHelperTask::class.java.getResourceAsStream("/db/DbMigration.java")!!
            .bufferedReader()
            .readText()
        destFile.writeText("package $packageName;\n\n")
        destFile.appendText(contents)
    }
}
