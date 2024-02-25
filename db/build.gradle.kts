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
    implementation("org.jooq:jooq-codegen:3.19.4")
    runtimeOnly("org.postgresql:postgresql:42.7.2")
    implementation("com.github.docker-java:docker-java-core:3.3.5")
    implementation("com.github.docker-java:docker-java-transport-zerodep:3.3.5")
}

gradlePlugin {
    plugins {
        register("db") {
            id = "dev.twarner.db"
            implementationClass = "dev.twarner.gradle.DbMigrationPlugin"
        }
    }
}
