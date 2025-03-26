package dev.twarner.gradle

import org.gradle.api.DefaultTask
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.provider.ListProperty
import org.gradle.api.provider.MapProperty
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputDirectory
import org.gradle.api.tasks.Optional
import org.gradle.api.tasks.TaskAction
import org.gradle.kotlin.dsl.getByName
import org.gradle.process.ExecOperations
import org.jetbrains.kotlin.gradle.targets.js.nodejs.NodeJsRootExtension
import org.jetbrains.kotlin.gradle.targets.js.yarn.yarn
import java.io.File
import javax.inject.Inject

abstract class YarnTask @Inject constructor(private val execOperations: ExecOperations) : DefaultTask() {
    @get:Input
    @get:Optional
    abstract val arguments: ListProperty<String>
    @get:InputDirectory
    abstract val dir: DirectoryProperty
    @get:Input
    abstract val environment: MapProperty<String, String>

    @TaskAction
    fun exec() {
        val nodeJs = project.rootProject.extensions.getByName<NodeJsRootExtension>("kotlinNodeJs").requireConfigured()
        val yarn = project.yarn.requireConfigured()

        //val modulesFolder = project.rootProject.layout.buildDirectory.dir("js/node_modules").get().asFile.absolutePath

        // TODO can I tell yarn to look in `target` for node_modules directly? Instead of symlinking
        // Update: I can with --modules-folder, but electron doesn't respect that and breaks when it tries to require things

        /*val link = dir.get().asFile.resolve("node_modules").toPath()
        val target = project.rootProject.layout.buildDirectory.dir("js/node_modules").get().asFile.toPath()
        Files.deleteIfExists(link)
        Files.createSymbolicLink(link, target)*/

        val nodeExecutable = nodeJs.executable

        val path = if (!yarn.ignoreScripts) {
            val nodePath = if (nodeJs.isWindows) {
                File(nodeExecutable).parent
            } else {
                nodeExecutable
            }
            "$nodePath${File.pathSeparator}${System.getenv("PATH")}"
        } else {
            null
        }

        val command = yarn.executable
        //val arguments = listOf("--modules-folder=$modulesFolder") + arguments.orNull.orEmpty()
        val arguments = arguments.orNull.orEmpty()

        execOperations.exec {
            if (path != null) {
                environment("PATH", path)
            }

            //environment("NODE_PATH", modulesFolder+":")

            environment(this@YarnTask.environment.orNull.orEmpty())

            if (yarn.standalone) {
                executable = command
                args = arguments
            } else {
                executable = nodeExecutable
                args = listOf(command) + arguments
            }

            workingDir = dir.get().asFile
        }.rethrowFailure()
    }
}
