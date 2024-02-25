plugins {
    `kotlin-dsl`
    `maven-publish`
    conventions.`jvm-target`
}

repositories {
    mavenCentral()
}

dependencies {
    implementation(projects.commonUtils)
}

gradlePlugin {
    plugins {
        register("sass") {
            id = "dev.twarner.sass"
            implementationClass = "dev.twarner.gradle.SassPlugin"
        }
    }
}
