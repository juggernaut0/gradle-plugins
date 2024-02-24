plugins {
    `kotlin-dsl`
    `maven-publish`
    conventions.`jvm-target`
}

repositories {
    gradlePluginPortal()
}

dependencies {
    implementation("com.google.cloud.tools:jib-gradle-plugin:3.4.0")
}

gradlePlugin {
    plugins {
        create("docker") {
            id = "dev.twarner.docker"
            implementationClass = "dev.twarner.gradle.DockerPlugin"
        }
    }
}
