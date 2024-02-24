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
