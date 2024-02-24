package dev.twarner.gradle

import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.artifacts.Configuration
import org.gradle.api.file.FileSystemLocation
import org.gradle.api.provider.Provider

fun <T: Any> Configuration.mapElements(fn: (FileSystemLocation) -> T): Provider<List<T>> {
    return this.incoming.files.elements.map { elems -> elems.map { fn(it) } }
}

fun Plugin<Project>.readManagedDependencies(folder: String): List<Pair<String, String>> {
    val resource = this::class.java.getResourceAsStream("/$folder/managedDependencies")
        ?: error("Unable to read resource '/$folder/managedDependencies'")
    return resource
        .bufferedReader()
        .readLines()
        .map { it.split("=").let { p -> p[0] to p[1] } }
}
