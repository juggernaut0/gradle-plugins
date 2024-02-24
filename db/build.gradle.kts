plugins {
    `kotlin-dsl`
    `maven-publish`
    conventions.`jvm-target`
}

repositories {
    mavenCentral()
}

dependencies {
    implementation("org.jooq:jooq-codegen:3.19.3")
    runtimeOnly("org.postgresql:postgresql:42.7.1")
    implementation("com.github.docker-java:docker-java-core:3.3.4")
    implementation("com.github.docker-java:docker-java-transport-zerodep:3.3.4")
}

gradlePlugin {
    plugins {
        register("db") {
            id = "dev.twarner.db"
            implementationClass = "dev.twarner.gradle.DbMigrationPlugin"
        }
    }
}
