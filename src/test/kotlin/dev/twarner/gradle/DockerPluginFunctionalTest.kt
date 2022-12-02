package dev.twarner.gradle

import org.gradle.testkit.runner.GradleRunner
import org.junit.jupiter.api.io.TempDir
import java.nio.file.Path
import kotlin.io.path.createDirectories
import kotlin.io.path.writeText
import kotlin.test.*

class DockerPluginFunctionalTest {
    @Test
    fun `can build an image`(@TempDir projectDir: Path) {
        projectDir.resolve("settings.gradle.kts").writeText(
            """
                rootProject.name = "test-proj"
            """.trimIndent()
        )

        projectDir.resolve("build.gradle.kts").writeText(
            """
                plugins {
                    id("dev.twarner.docker")
                    application
                }
                
                version = "1.0-SNAPSHOT"
                
                application {
                    mainClass.set("Main")
                }
            """.trimIndent()
        )

        projectDir.resolve("src/main/java")
            .also { it.createDirectories() }
            .resolve("Main.java")
            .also {
                it.writeText(
                    """
                        public class Main {
                            public static void main(String[] args) {
                                System.out.println("Hello");
                            }
                        }
                    """.trimIndent()
                )
            }

        val result = GradleRunner.create()
            .withProjectDir(projectDir.toFile())
            .withPluginClasspath()
            .withArguments("dockerBuild")
            .forwardOutput()
            .build()

        assertContains(result.output, "juggernaut0/test-proj:SNAPSHOT")
    }
}
