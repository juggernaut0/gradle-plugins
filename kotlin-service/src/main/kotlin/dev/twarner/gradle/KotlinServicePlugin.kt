package dev.twarner.gradle

import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.attributes.Usage
import org.gradle.api.tasks.Copy
import org.gradle.api.tasks.SourceSetContainer
import org.gradle.kotlin.dsl.configure
import org.gradle.kotlin.dsl.named
import org.gradle.kotlin.dsl.register

class KotlinServicePlugin : Plugin<Project> {
    override fun apply(project: Project) {
        project.pluginManager.apply("org.jetbrains.kotlin.jvm")
        project.pluginManager.apply("application")
        project.pluginManager.apply(DockerPlugin::class.java)

        val webResourceConfiguration = project.configurations.create("webResource") {
            isCanBeConsumed = false
            isCanBeResolved = true
            attributes {
                attribute(Usage.USAGE_ATTRIBUTE, project.objects.named("web-resource"))
            }
        }

        val webResourcesDir = project.layout.buildDirectory.dir("webResources")
        val copyWebResources = project.tasks.register<Copy>("copyWebResources") {
            from(webResourceConfiguration.mapElements { project.zipTree(it) })
            into(webResourcesDir.map { it.dir("static") })
        }

        project.tasks.named("processResources") {
            dependsOn(copyWebResources)
        }

        project.configure<SourceSetContainer> {
            named("main") {
                resources.srcDir(webResourcesDir)
            }
        }
    }
}
