plugins {
    `kotlin-dsl`
    `maven-publish`
}

repositories {
    gradlePluginPortal()
}

dependencies {
    implementation("de.undercouch:gradle-download-task:5.3.1")
}

group = "dev.twarner"
val projectVersion: String by rootProject
version = projectVersion
publishing {
    repositories {
        maven {
            name = "pages"
            url = uri("$rootDir/pages/m2/repository")
        }
    }
}
