plugins {
    `kotlin-dsl`
    `maven-publish`
}

group = "dev.twarner"
val projectVersion: String by rootProject
version = projectVersion

repositories {
    gradlePluginPortal()
}

dependencies {
    implementation(kotlin("gradle-plugin"))
}
