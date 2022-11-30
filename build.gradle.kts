import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    `java-gradle-plugin`
    `kotlin-dsl`
    `maven-publish`
}

group = "dev.twarner"
val projectVersion: String by rootProject
version = projectVersion

repositories {
    mavenCentral()
    gradlePluginPortal()
}

dependencies {
    implementation("de.undercouch:gradle-download-task:5.3.0")
    implementation("com.bmuschko.docker-remote-api:com.bmuschko.docker-remote-api.gradle.plugin:9.0.1")

    testImplementation(kotlin("test"))
}

gradlePlugin {
    plugins {
        create("docker") {
            id = "dev.twarner.docker"
            implementationClass = "dev.twarner.gradle.DockerPlugin"
        }
    }
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

