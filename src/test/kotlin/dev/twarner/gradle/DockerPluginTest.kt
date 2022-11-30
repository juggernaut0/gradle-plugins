package dev.twarner.gradle

import org.gradle.testfixtures.ProjectBuilder
import kotlin.test.Test

class DockerPluginTest {
    @Test
    fun pluginApplies() {
        val project = ProjectBuilder.builder().build()
        project.pluginManager.apply(DockerPlugin::class.java)
    }
}