import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    `java-gradle-plugin`
    `kotlin-dsl`
    `maven-publish`
}

allprojects {
    group = "dev.twarner.gradle"
}

repositories {
    mavenCentral()
    gradlePluginPortal()
}

dependencies {
    implementation("de.undercouch:gradle-download-task:5.4.0")
    implementation("com.bmuschko.docker-remote-api:com.bmuschko.docker-remote-api.gradle.plugin:9.3.0")

    testImplementation(kotlin("test"))
}

gradlePlugin {
    plugins {
        create("settings") {
            id = "dev.twarner.settings"
            implementationClass = "dev.twarner.gradle.SettingsPlugin"
        }
    }
}

tasks {
    test {
        useJUnitPlatform()
    }

    withType<KotlinCompile>().configureEach {
        kotlinOptions.jvmTarget = "17"
    }

    val collectPluginIds by registering {
        outputs.upToDateWhen { false }
        val outputFile = temporaryDir.resolve("managedIds.txt")
        outputs.file(outputFile)
        val ids = mutableListOf<String>()
        allprojects {
            pluginManager.withPlugin("java-gradle-plugin") {
                configure<GradlePluginDevelopmentExtension> {
                    ids.addAll(plugins.map { it.id })
                }
            }
        }
        doLast {
            outputFile.writeText(ids.joinToString(separator = "\n", postfix = "\n"))
        }
    }

    val generateBuildInfo by registering {
        val outputFile = layout.buildDirectory.file("build-info.properties")
        inputs.property("version", version.toString())
        outputs.file(outputFile)
        doLast {
            outputFile.get().asFile.writeText("version=$version")
        }
    }

    processResources {
        from(generateBuildInfo)
        from(collectPluginIds)
    }
}
