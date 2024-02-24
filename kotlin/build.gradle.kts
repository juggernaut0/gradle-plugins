plugins {
    `kotlin-dsl`
    `maven-publish`
}

group = "dev.twarner"

repositories {
    gradlePluginPortal()
}

dependencies {
    implementation(kotlin("gradle-plugin"))
}
