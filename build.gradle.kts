import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    kotlin("jvm") version "1.7.22"
    `java-gradle-plugin`
    `kotlin-dsl`
    `maven-publish`
}

group = "dev.twarner"
val projectVersion: String by rootProject
version = projectVersion

repositories {
    mavenCentral()
}

dependencies {
    implementation("de.undercouch:gradle-download-task:5.3.0")

    testImplementation(kotlin("test"))
}

publishing {
    repositories {
        maven {
            name = "pages"
            url = uri("$rootDir/pages/m2/repository")
        }
    }
}

tasks.test {
    useJUnitPlatform()
}

tasks.withType<KotlinCompile> {
    kotlinOptions.jvmTarget = "17"
}

