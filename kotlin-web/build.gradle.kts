plugins {
    `kotlin-dsl`
    `maven-publish`
    conventions.`jvm-target`
}

repositories {
    gradlePluginPortal()
    mavenCentral()
}

dependencies {
    implementation(kotlin("gradle-plugin"))
    implementation(projects.sass)
    implementation(projects.downloadFirefox)
}

gradlePlugin {
    plugins {
        register("kotlin-web") {
            id = "dev.twarner.kotlin-web"
            implementationClass = "dev.twarner.gradle.KotlinWebPlugin"
        }
    }
}
