package dev.twarner.gradle

import com.bmuschko.gradle.docker.DockerRemoteApiPlugin
import com.bmuschko.gradle.docker.tasks.image.DockerBuildImage
import com.bmuschko.gradle.docker.tasks.image.Dockerfile
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.model.ObjectFactory
import org.gradle.api.plugins.ApplicationPlugin
import org.gradle.api.tasks.Sync
import org.gradle.api.tasks.TaskProvider
import org.gradle.api.tasks.bundling.Tar
import org.gradle.kotlin.dsl.named
import org.gradle.kotlin.dsl.property

class DockerPlugin : Plugin<Project> {
    override fun apply(target: Project) {
        return with(target) {
            pluginManager.apply(DockerRemoteApiPlugin::class.java)
            pluginManager.apply(ApplicationPlugin::class.java)

            val extension = extensions.create("dockerApplication", Extension::class.java)

            val syncContext = registerSyncContextTask()
            val dockerfile = registerDockerfileTask(extension, syncContext)
            registerDockerBuildTask(extension, dockerfile)
        }
    }

    private fun Project.registerSyncContextTask(): TaskProvider<Sync> {
        return tasks.register("dockerSyncContext", Sync::class.java) {
            val distTar = tasks.named<Tar>("distTar")
            dependsOn(distTar)
            from(distTar.flatMap { it.archiveFile })
            into(layout.buildDirectory.dir("docker"))
        }
    }

    private fun Project.registerDockerfileTask(extension: Extension, syncContext: TaskProvider<Sync>): TaskProvider<Dockerfile> {
        return tasks.register("dockerfile", Dockerfile::class.java) {
            dependsOn(syncContext)
            val distTar = tasks.named<Tar>("distTar")
            from(extension.baseImage.map { Dockerfile.From(it) })
            addFile(distTar.flatMap { it.archiveFileName }.map { Dockerfile.File(it, "/app/") })
            defaultCommand(distTar.flatMap { it.archiveFile }.map { it.asFile.nameWithoutExtension }.map { listOf("/app/$it/bin/${project.name}") })
        }
    }

    private fun Project.registerDockerBuildTask(extension: Extension, dockerfile: TaskProvider<Dockerfile>): TaskProvider<DockerBuildImage> {
        return tasks.register("dockerBuild", DockerBuildImage::class.java) {
            dependsOn(dockerfile)

            if (version.toString().endsWith("SNAPSHOT")) {
                images.add(extension.tagBase.map { tagBase -> "$tagBase${rootProject.name}:SNAPSHOT" })
            } else {
                images.add(extension.tagBase.map { tagBase -> "$tagBase${rootProject.name}:$version" })
            }
        }
    }

    open class Extension(objects: ObjectFactory) {
        val baseImage = objects.property<String>()
        val tagBase = objects.property<String>()

        init {
            baseImage.convention("eclipse-temurin:17")
            tagBase.convention("juggernaut0/")
        }
    }
}
