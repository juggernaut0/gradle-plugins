package dev.twarner.gradle

import dev.twarner.gradle.openapi.GenerateApiTask
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.dependencies
import org.gradle.kotlin.dsl.maven
import org.gradle.kotlin.dsl.repositories

class KotlinApiPlugin : Plugin<Project> {
    override fun apply(project: Project) {
        project.pluginManager.apply("org.jetbrains.kotlin.multiplatform")
        project.pluginManager.apply("org.jetbrains.kotlin.plugin.serialization")

        project.repositories {
            mavenCentral()
            maven("https://juggernaut0.github.io/m2/repository")
        }

        val kotlinExt = project.extensions.getByType(org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension::class.java)
        kotlinExt.apply {
            js {
                browser()
            }
            jvm()
        }

        project.dependencies {
            for ((config, dep) in readManagedDependencies("kotlin-api")) {
                add(config, dep)
            }
        }

        val generateApiTask = project.tasks.register("generateApi", GenerateApiTask::class.java) {
            inputSpec.set(project.projectDir.resolve("openapi.yaml"))
            packageName.set("${project.group}.api")
            outputDir.set(project.layout.buildDirectory.dir("generated/source/api"))
        }

        kotlinExt.sourceSets.named("commonMain") {
            kotlin.srcDir(generateApiTask.flatMap { it.outputDir })
        }
    }
}
