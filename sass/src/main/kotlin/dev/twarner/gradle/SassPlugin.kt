package dev.twarner.gradle

import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.attributes.Category
import org.gradle.api.attributes.Usage
import org.gradle.api.component.SoftwareComponentFactory
import org.gradle.api.publish.PublishingExtension
import org.gradle.api.publish.maven.MavenPublication
import org.gradle.api.tasks.Copy
import org.gradle.api.tasks.bundling.Zip
import org.gradle.kotlin.dsl.*
import org.gradle.kotlin.dsl.support.serviceOf

class SassPlugin : Plugin<Project> {
    override fun apply(project: Project) {
        val sassUsageAttribute = project.objects.named(Usage::class, "sass-api")
        val sassConfig = project.configurations.create("sass") {
            isCanBeConsumed = false
            isCanBeResolved = true
            attributes {
                attribute(Usage.USAGE_ATTRIBUTE, sassUsageAttribute)
                attribute(Category.CATEGORY_ATTRIBUTE, project.objects.named(Category.LIBRARY))
            }
        }

        val sassApiElementsConfigration = project.configurations.create("sassApiElements") {
            isCanBeConsumed = true
            isCanBeResolved = false
            attributes {
                attribute(Usage.USAGE_ATTRIBUTE, sassUsageAttribute)
                attribute(Category.CATEGORY_ATTRIBUTE, project.objects.named(Category.LIBRARY))
            }
        }

        val sassSrc = project.layout.projectDirectory.dir("src/sass")

        val assembleSassSrc by project.tasks.registering(Copy::class) {
            from(sassSrc)
            from(sassConfig.mapElements { file -> project.zipTree(file).matching { include("**/*.scss") } })
            includeEmptyDirs = false
            into(project.layout.buildDirectory.dir("sassSrc"))
        }

        val runSass by project.tasks.registering(SassTask::class) {
            dependsOn(assembleSassSrc)
            version.convention("1.70.0")
            inputDir.set(project.layout.dir(assembleSassSrc.map { it.destinationDir }))
            outputDir.convention(project.layout.buildDirectory.dir("sass"))
        }

        val sassZip = project.tasks.register<Zip>("sassZip") {
            from(sassSrc)
            destinationDirectory.set(project.layout.buildDirectory.dir("libs"))
            archiveBaseName.set("${project.name}-sass")
        }

        project.artifacts {
            add(sassApiElementsConfigration.name, sassZip)
        }

        project.pluginManager.withPlugin("maven-publish") {
            val factory = project.serviceOf<SoftwareComponentFactory>()
            val comp = factory.adhoc("sass")
            project.components.add(comp)
            comp.addVariantsFromConfiguration(sassApiElementsConfigration) {
                mapToMavenScope("compile")
            }

            project.extensions.getByType<PublishingExtension>().apply {
                publications {
                    create<MavenPublication>("sass") {
                        from(comp)
                        artifactId = "${project.name}-sass"
                    }
                }
            }
        }
    }
}
