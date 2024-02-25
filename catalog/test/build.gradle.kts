plugins {
    kotlin("jvm")
}

repositories {
    mavenCentral()
}

val catalogForTest = configurations.create("catalogForTest") {
    isCanBeConsumed = false
    isCanBeResolved = true
    attributes {
        attribute(Category.CATEGORY_ATTRIBUTE, objects.named(Category.REGULAR_PLATFORM))
        attribute(Usage.USAGE_ATTRIBUTE, objects.named(Usage.VERSION_CATALOG))
    }
}

dependencies {
    testImplementation(kotlin("test"))
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
    testImplementation("com.fasterxml.jackson.dataformat:jackson-dataformat-toml:2.14.0")
    testImplementation(gradleTestKit())

    catalogForTest(projects.catalog)
}

tasks.test {
    useJUnitPlatform()
    inputs.files(catalogForTest)
    doFirst {
        systemProperty("tomlLocation", catalogForTest.files.single().absolutePath)
    }
}
