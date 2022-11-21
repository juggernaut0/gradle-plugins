import org.gradle.api.plugins.catalog.internal.TomlFileGenerator

plugins {
    kotlin("jvm")
}

repositories {
    mavenCentral()
}

dependencies {
    testImplementation(kotlin("test"))
    testImplementation("com.fasterxml.jackson.dataformat:jackson-dataformat-toml:2.14.0")
    testImplementation(gradleTestKit())
}

tasks.test {
    val generateCatalogAsToml = parent!!.tasks.named<TomlFileGenerator>("generateCatalogAsToml")
    dependsOn(generateCatalogAsToml)
    useJUnitPlatform()
    inputs.file(generateCatalogAsToml.flatMap { it.outputFile })
    doFirst {
        systemProperty("tomlLocation", generateCatalogAsToml.flatMap { it.outputFile }.get())
    }
}
