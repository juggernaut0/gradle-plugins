plugins {
    `kotlin-dsl`
    `maven-publish`
    conventions.`jvm-target`
}

repositories {
    mavenCentral()
    gradlePluginPortal()
}

dependencies {
    implementation(kotlin("gradle-plugin"))
    implementation(kotlin("serialization"))
    implementation(projects.downloadFirefox)
    implementation(projects.commonUtils)
    implementation("io.swagger.parser.v3:swagger-parser:2.1.20")
    implementation("com.squareup:kotlinpoet:1.16.0")
}

gradlePlugin {
    plugins {
        register("kotlin-api") {
            id = "dev.twarner.kotlin-api"
            implementationClass = "dev.twarner.gradle.KotlinApiPlugin"
        }
    }
}
