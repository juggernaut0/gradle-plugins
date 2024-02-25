package dev.twarner.gradle

import com.google.cloud.tools.jib.gradle.JibExtension
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.configure

class DockerPlugin : Plugin<Project> {
    override fun apply(project: Project) {
        project.pluginManager.apply("com.google.cloud.tools.jib")

        project.configure<JibExtension> {
            from.image = "eclipse-temurin:21-jre"
            to.setImage(project.provider {
                if (project.version.toString().endsWith("SNAPSHOT")) {
                    "${project.rootProject.name}:SNAPSHOT"
                } else {
                    "juggernaut0/${project.rootProject.name}:${project.version}"
                }
            })
        }
    }
}
