package dev.twarner.gradle

import com.github.dockerjava.core.DefaultDockerClientConfig
import com.github.dockerjava.core.DockerClientImpl
import com.github.dockerjava.zerodep.ZerodepDockerHttpClient
import org.gradle.api.DefaultTask
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.TaskAction

abstract class StartTestDbTask : DefaultTask() {
    @get:Input
    abstract val databaseName: Property<String>

    @get:Input
    abstract val postgresVersion: Property<String>

    @TaskAction
    fun start() {
        val config = DefaultDockerClientConfig.createDefaultConfigBuilder().build();
        val httpClient = ZerodepDockerHttpClient.Builder().dockerHost(config.dockerHost).build()
        val dockerClient = DockerClientImpl.getInstance(config, httpClient)

        val existing = dockerClient.listContainersCmd().withNameFilter(listOf("test-db")).exec().firstOrNull()

        val postgres = if (existing == null) {
            logger.quiet("Starting new test DB...")
            PostgresContainer.new(dockerClient, postgresVersion.get(), "test-db", publish5432 = true)
        } else {
            PostgresContainer.fromExisting(dockerClient, existing.id)
        }
        postgres.waitForReady()

        val dbName = databaseName.get()
        postgres.psql("""
            DROP DATABASE IF EXISTS $dbName;
            DROP ROLE IF EXISTS $dbName;

            CREATE USER $dbName WITH PASSWORD '$dbName';
            CREATE DATABASE $dbName WITH OWNER = $dbName;
            GRANT ALL PRIVILEGES ON DATABASE $dbName TO $dbName;
        """.trimIndent())
    }
}
