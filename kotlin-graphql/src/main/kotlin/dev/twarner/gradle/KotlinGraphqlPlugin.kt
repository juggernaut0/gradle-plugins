package dev.twarner.gradle

import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.file.SourceDirectorySet
import org.gradle.api.tasks.SourceSetContainer
import org.gradle.kotlin.dsl.configure
import org.gradle.kotlin.dsl.getByName

class KotlinGraphqlPlugin : Plugin<Project> {
    override fun apply(project: Project) {
        val generateResolverStubs = project.tasks.register("generateResolverStubs", GenerateResolverStubsTask::class.java) {
            group = "kotlin-graphql"
            description = "Generate resolver stubs from GraphQL schema files"
            inputSchemas.from(project.fileTree("src/main/graphql") { include("**/*.graphqls") })
            packageName.set("${project.group}.graphql.resolvers")
            outputDir.set(project.layout.buildDirectory.dir("generated/source/graphql"))
        }

        project.pluginManager.withPlugin("org.jetbrains.kotlin.jvm") {
            project.configure<SourceSetContainer> {
                named("main") {
                    extensions.getByName<SourceDirectorySet>("kotlin")
                        .srcDir(generateResolverStubs.flatMap { it.outputDir })
                }
            }
        }
    }
}
