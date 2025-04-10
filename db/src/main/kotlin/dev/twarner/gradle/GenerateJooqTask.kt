package dev.twarner.gradle

import org.gradle.api.DefaultTask
import org.gradle.api.file.ConfigurableFileCollection
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Classpath
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputFiles
import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.tasks.TaskAction
import org.gradle.process.ExecOperations
import org.jooq.codegen.GenerationTool
import org.jooq.meta.jaxb.*
import org.jooq.meta.jaxb.Target
import javax.inject.Inject

abstract class GenerateJooqTask @Inject constructor(private val execOperations: ExecOperations) : DefaultTask() {
    @get:InputFiles
    @get:Classpath
    abstract val migrationClasspath: ConfigurableFileCollection

    @get:Input
    abstract val packageName: Property<String>

    @get:Input
    abstract val migratorMainClass: Property<String>

    @get:Input
    abstract val postgresVersion: Property<String>

    @get:OutputDirectory
    abstract val generatedSrcDir: DirectoryProperty

    private val dockerClientProvider: DockerClientProvider = project.objects.newInstance(DockerClientProvider::class.java)

    @TaskAction
    fun generate() {
        createContainer().use {
            it.waitForReady()
            val url = it.getJdbcUrl()

            runFlyway(url, "postgres", "postgres")
            runJooq(url, "postgres", "postgres")
        }
    }

    private fun createContainer(): PostgresContainer {
        val dockerClient = dockerClientProvider.newDockerClient()
        return PostgresContainer.new(dockerClient, postgresVersion.get(), "jooq-postgres")
    }

    private fun runFlyway(url: String, username: String, password: String) {
        execOperations.javaexec {
            mainClass.set(migratorMainClass)
            classpath = migrationClasspath
            args = listOf(url, username, password)
        }
    }

    private fun runJooq(url: String, username: String, password: String) {
        generatedSrcDir.get().asFile.deleteRecursively()

        val jooqConfig = jooqConfig()
        jooqConfig.jdbc.apply {
            this.url = url
            this.username = username
            this.password = password
        }
        GenerationTool.generate(jooqConfig)
    }

    private fun jooqConfig(): Configuration {
        return Configuration()
            .withJdbc(Jdbc().withDriver("org.postgresql.Driver"))
            .withGenerator(
                Generator()
                    .withName("org.jooq.codegen.DefaultGenerator")
                    .withStrategy(Strategy().withName("org.jooq.codegen.DefaultGeneratorStrategy"))
                    .withDatabase(
                        Database()
                            .withName("org.jooq.meta.postgres.PostgresDatabase")
                            .withInputSchema("public")
                            .withIncludes(".*")
                            .withExcludes("flyway_schema_history")
                    )
                    .withGenerate(
                        Generate()
                            .withDeprecated(false)
                            .withFluentSetters(false)
                            .withRecords(true)
                            .withRelations(true)
                    )
                    .withTarget(
                        Target()
                            .withPackageName(packageName.get())
                            .withDirectory(generatedSrcDir.get().asFile.absolutePath)
                    )
            )
    }
}
