package dev.twarner.gradle

import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.plugins.JavaLibraryPlugin
import org.gradle.kotlin.dsl.dependencies
import org.gradle.kotlin.dsl.repositories

class DbMigrationPlugin : Plugin<Project> {
    override fun apply(project: Project) {
        project.pluginManager.apply(JavaLibraryPlugin::class.java)

        project.repositories {
            mavenCentral()
        }

        project.dependencies {
            for ((config, dep) in readManagedDependencies("db")) {
                add(config, dep)
            }
        }

        val generateMigrationHelperTask = project.tasks.register("generateMigrationHelper", GenerateMigrationHelperTask::class.java) {
            description = "Generates migration helper"
            packageName.set("${project.group}.db")
            outputDir.set(project.layout.buildDirectory.dir("generated/source/migration"))
        }

        val sourceSets = project.extensions.getByType(org.gradle.api.plugins.JavaPluginExtension::class.java).sourceSets
        val mainSourceSet = sourceSets.getByName("main")
        val migrateSourceSet = sourceSets.create("migrate") {
            compileClasspath += project.configurations.getByName("compileClasspath")
            runtimeClasspath += project.configurations.getByName("runtimeClasspath")
            java {
                srcDir(mainSourceSet.java.srcDirs)
                srcDir(generateMigrationHelperTask.map { it.outputDir })
            }
            resources {
                srcDir(mainSourceSet.resources.srcDirs)
            }
        }

        val generateJooqTask = project.tasks.register("generateJooq", GenerateJooqTask::class.java) {
            description = "Generates jooq classes"
            migrationClasspath.setFrom(migrateSourceSet.runtimeClasspath)
            migratorMainClass.set(generateMigrationHelperTask.flatMap { it.migratorMainCLass })
            packageName.set("${project.group}.db.jooq")
            postgresVersion.set("16")
            generatedSrcDir.set(project.layout.buildDirectory.dir("generated/source/jooq"))
        }

        mainSourceSet.java {
            srcDir(generateMigrationHelperTask.map { it.outputDir })
            srcDir(generateJooqTask.map { it.generatedSrcDir })
        }

        project.tasks.register("startTestDb", StartTestDbTask::class.java) {
            description = "Starts a test database"
            databaseName.set(project.group.toString())
            postgresVersion.set("16")
        }
    }
}
