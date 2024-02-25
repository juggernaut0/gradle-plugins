plugins {
    `kotlin-dsl`
    `maven-publish`
    conventions.`jvm-target`
}

repositories {
    gradlePluginPortal()
}

dependencies {
    implementation(kotlin("gradle-plugin"))
    implementation(projects.docker)
    implementation(projects.commonUtils)
}

gradlePlugin {
    plugins {
        register("kotlin-service") {
            id = "dev.twarner.kotlin-service"
            implementationClass = "dev.twarner.gradle.KotlinServicePlugin"
        }
    }
}
