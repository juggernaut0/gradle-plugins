package dev.twarner.gradle

import org.gradle.api.Plugin
import org.gradle.api.initialization.Settings
import org.gradle.kotlin.dsl.maven
import java.util.Properties

class SettingsPlugin : Plugin<Settings> {
    override fun apply(settings: Settings) {
        settings.dependencyResolutionManagement {
            repositories {
                if (currentVersion.endsWith("SNAPSHOT")) {
                    mavenLocal()
                }
                maven("https://juggernaut0.github.io/m2/repository")
            }

            versionCatalogs {
                create("libs") {
                    from("dev.twarner.gradle:catalog:$currentVersion")
                }
            }
        }

        settings.pluginManagement {
            resolutionStrategy {
                eachPlugin {
                    if (requested.id.id in managedIds) {
                        useVersion(currentVersion)
                    }
                }
            }
        }
    }

    private companion object {
        val currentVersion: String
        val managedIds: List<String>

        init {
            val buildInfo = this::class.java.getResourceAsStream("/build-info.properties")
            val buildInfoProps = Properties().also { it.load(buildInfo) }
            currentVersion = buildInfoProps.getProperty("version")

            val idsFile = this::class.java.getResourceAsStream("/managedIds.txt") ?: error("Missing managedIds file")
            managedIds = idsFile.bufferedReader().readLines()
        }
    }
}
