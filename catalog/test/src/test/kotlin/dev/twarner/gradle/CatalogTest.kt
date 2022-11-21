package dev.twarner.gradle

import com.fasterxml.jackson.databind.node.ObjectNode
import com.fasterxml.jackson.dataformat.toml.TomlMapper
import org.gradle.testkit.runner.GradleRunner
import org.junit.jupiter.api.io.TempDir
import java.io.File
import java.nio.file.Path
import kotlin.io.path.createDirectories
import kotlin.io.path.writeText
import kotlin.test.Test

class CatalogTest {
    private companion object {
        private val tomlLocation = System.getProperty("tomlLocation")!!
        private val libsKeys: List<String>

        init {
            val mapper = TomlMapper()
            val node = mapper.readTree(File(tomlLocation))
            libsKeys = node["libraries"].let { it as ObjectNode }.fieldNames().asSequence().toList()
                .filterNot { it.startsWith("twarner") } // local libs won't be able to resolve
                .map { it.replace('-', '.') }
        }
    }

    @Test
    fun `libs resolve`(@TempDir projectDir: Path) {
        projectDir.resolve("settings.gradle.kts").writeText(
            """
            dependencyResolutionManagement {
                versionCatalogs {
                    create("libs") {
                        from(files("$tomlLocation"))
                    }
                }
            }
            """.trimIndent()
        )

        projectDir.resolve("build.gradle.kts").writeText(
            buildString {
                appendLine(
                    """
                    plugins {
                        java
                    }
                    
                    repositories {
                        mavenCentral()
                        maven("https://juggernaut0.github.io/m2/repository")
                    }
                    
                    dependencies {
                    """.trimIndent()
                )
                for (lib in libsKeys) {
                    if (lib.endsWith(".bom")) {
                        appendLine("    implementation(platform(libs.$lib))")
                    } else {
                        appendLine("    implementation(libs.$lib)")
                    }
                }
                appendLine("}")
            }.also { println(it) }
        )

        projectDir.resolve("src/main/java")
            .also { it.createDirectories() }
            .resolve("Main.java")
            .writeText("public class Main { }")

        GradleRunner.create()
            .withProjectDir(projectDir.toFile())
            .withArguments("classes")
            .forwardOutput()
            .build()
    }
}
