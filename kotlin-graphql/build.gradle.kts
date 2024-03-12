plugins {
    `kotlin-dsl`
    `maven-publish`
    conventions.`jvm-target`
}

repositories {
    mavenCentral()
}

dependencies {
    implementation("com.graphql-java:graphql-java:21.3")
    implementation("com.squareup:kotlinpoet:1.16.0")

    testImplementation(kotlin("test"))
}

gradlePlugin {
    plugins {
        register("kotlin-graphql") {
            id = "dev.twarner.kotlin-graphql"
            implementationClass = "dev.twarner.gradle.KotlinGraphqlPlugin"
        }
    }
}

tasks.test {
    useJUnitPlatform()
}
