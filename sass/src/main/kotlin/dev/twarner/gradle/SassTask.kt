package dev.twarner.gradle

import org.gradle.api.DefaultTask
import org.gradle.api.GradleException
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.*
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.nio.file.Path
import kotlin.io.path.createDirectories
import kotlin.io.path.exists

abstract class SassTask : DefaultTask() {
    @get:InputDirectory
    abstract val inputDir: DirectoryProperty

    @get:OutputDirectory
    abstract val outputDir: DirectoryProperty

    @get:Input
    abstract val version: Property<String>

    init {
        onlyIf { inputDir.get().asFile.let { it.exists() && it.isDirectory } }
    }

    @TaskAction
    fun run() {
        val version = version.get()
        val gradleHome = project.gradle.gradleUserHomeDir
        val sassDir = "$gradleHome/sass/$version"
        val downloadDest = Path.of(sassDir, "sass.tar.gz")
        val unpackDest = "$sassDir/sass"

        if (!downloadDest.exists()) {
            downloadDest.parent.createDirectories()

            val os = when (System.getProperty("os.name")) {
                "Mac OS X" -> "macos"
                "Windows" -> "windows"
                "Linux" -> "linux"
                else -> error("Unknown os.name ${System.getProperty("os.name")}")
            }

            val httpClient = HttpClient.newBuilder().followRedirects(HttpClient.Redirect.ALWAYS).build()
            val response = httpClient.send(
                HttpRequest.newBuilder()
                    .uri(URI("https://github.com/sass/dart-sass/releases/download/$version/dart-sass-$version-$os-x64.tar.gz"))
                    .GET()
                    .build(),
                HttpResponse.BodyHandlers.ofFile(downloadDest),
            )

            if (response.statusCode() >= 300) {
                throw GradleException("Failed to download dart-sass: ${response.statusCode()} ${response.body()}")
            }
        }

        project.copy {
            from(project.tarTree(downloadDest))
            into(unpackDest)
        }

        project.exec {
            executable("$unpackDest/dart-sass/sass")
            args("--embed-sources", "${inputDir.get()}:${outputDir.get()}")
        }
    }
}
