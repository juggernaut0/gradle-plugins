package dev.twarner.gradle

import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.attributes.Usage
import org.gradle.api.tasks.bundling.Zip
import org.gradle.kotlin.dsl.configure
import org.gradle.kotlin.dsl.dependencies
import org.gradle.kotlin.dsl.named
import org.gradle.kotlin.dsl.register
import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension
import org.jetbrains.kotlin.gradle.targets.js.webpack.KotlinWebpack

class KotlinWebPlugin : Plugin<Project> {
    override fun apply(project: Project) {
        project.pluginManager.apply("dev.twarner.common")
        project.pluginManager.apply("dev.twarner.download-firefox")
        project.pluginManager.apply("org.jetbrains.kotlin.multiplatform")
        project.pluginManager.apply(SassPlugin::class.java)

        project.configure<KotlinMultiplatformExtension> {
            js {
                browser {
                    testTask {
                        useKarma {
                            useFirefoxHeadless()
                        }
                        if (System.getProperty("os.name").contains("Mac")) {
                            doFirst {
                                environment("FIREFOX_BIN", "/Applications/Firefox.app/Contents/MacOS/firefox")
                            }
                        } else {
                            val downloadFirefox = project.tasks.named<DownloadFirefoxTask>("downloadFirefox")
                            dependsOn(downloadFirefox)
                            doFirst {
                                environment("FIREFOX_BIN", downloadFirefox.flatMap { it.outputBin }.get().asFile.absolutePath)
                            }
                        }
                    }
                }
                binaries.executable()
            }
        }

        val webResourceConfiguration = project.configurations.create("webResource") {
            isCanBeConsumed = true
            isCanBeResolved = false
            attributes {
                attribute(Usage.USAGE_ATTRIBUTE, project.objects.named("web-resource"))
            }
        }

        val devWebpackTask = project.tasks.named<KotlinWebpack>("jsBrowserDevelopmentWebpack")
        val prodWebpackTask = project.tasks.named<KotlinWebpack>("jsBrowserProductionWebpack")

        val distZip = project.tasks.register<Zip>("distZip") {
            val webpackTask = if (project.version.toString().endsWith("SNAPSHOT")) {
                devWebpackTask
            } else {
                prodWebpackTask
            }

            from(project.tasks.named("runSass"))
            from(webpackTask.map { it.outputDirectory })
        }

        project.artifacts {
            add(webResourceConfiguration.name, distZip)
        }

        // Hack to fix kotlin plugin running both webpacks on `gradle build`
        project.afterEvaluate {
            val disabledWebpackMode = if (project.version.toString().endsWith("SNAPSHOT")) "Production" else "Development"
            val tasksToDisable = listOf(
                "compile${disabledWebpackMode}ExecutableKotlinJs",
                "js${disabledWebpackMode}ExecutableCompileSync",
                "jsBrowser${disabledWebpackMode}Webpack",
            )
            for (taskName in tasksToDisable) {
                project.tasks.named(taskName) {
                    enabled = false
                }
            }
        }
    }
}
