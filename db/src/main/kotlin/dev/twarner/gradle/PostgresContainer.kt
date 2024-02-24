package dev.twarner.gradle

import com.github.dockerjava.api.DockerClient
import com.github.dockerjava.api.async.ResultCallback
import com.github.dockerjava.api.command.PullImageResultCallback
import com.github.dockerjava.api.model.ExposedPort
import com.github.dockerjava.api.model.Frame
import com.github.dockerjava.api.model.HostConfig
import com.github.dockerjava.api.model.PortBinding
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.io.ByteArrayInputStream
import java.io.InputStream

class PostgresContainer private constructor(
    private val client: DockerClient,
    private val containerId: String,
) : AutoCloseable {
    fun waitForReady() {
        repeat(20) {
            val exitCode = exec("pg_isready", "-h", "127.0.0.1")
            if (exitCode == 0L) {
                return
            }
            Thread.sleep(1000)
        }
        throw IllegalStateException("Postgres container did not become ready")
    }

    fun psql(cmd: String) {
        exec("psql", "-U", "postgres", stdIn = ByteArrayInputStream(("$cmd\n\\q\n").toByteArray()))
    }

    private fun exec(vararg cmd: String, stdIn: InputStream? = null): Long {
        val execCreateCmdResponse = client.execCreateCmd(containerId)
            .withAttachStderr(true)
            .withAttachStdout(true)
            .also { if (stdIn != null) it.withAttachStdin(true) }
            .withCmd(*cmd)
            .exec()
        client.execStartCmd(execCreateCmdResponse.id)
            .also { if (stdIn != null) it.withStdIn(stdIn) }
            .exec(ExecLogger(LoggerFactory.getLogger(PostgresContainer::class.java)))
            .awaitCompletion()
        return client.inspectExecCmd(execCreateCmdResponse.id).exec().exitCodeLong
    }

    override fun close() {
        client.stopContainerCmd(containerId).exec()
    }

    fun getJdbcUrl(dbName: String = "postgres"): String {
        val inspect = client.inspectContainerCmd(containerId).exec()
        val port = inspect.networkSettings.ports.bindings[ExposedPort(5432)]!!.first()
        return "jdbc:postgresql://${port.hostIp}:${port.hostPortSpec}/$dbName"
    }

    companion object {
        fun new(
            client: DockerClient,
            version: String,
            containerName: String,
            publish5432: Boolean = false,
        ): PostgresContainer {
            val tag = "postgres:$version"
            client.pullImageCmd(tag).exec(PullImageResultCallback()).awaitCompletion()

            val hostConfig = HostConfig.newHostConfig().withAutoRemove(true)
            if (publish5432) {
                hostConfig.withPortBindings(PortBinding.parse("5432:5432"))
            } else {
                hostConfig.withPublishAllPorts(true)
            }

            val resp = client.createContainerCmd(tag)
                .withName(containerName)
                .withEnv("POSTGRES_USER=postgres", "POSTGRES_PASSWORD=postgres")
                .withHostConfig(hostConfig)
                .exec()

            val containerId = resp.id

            client.startContainerCmd(containerId).exec()

            return PostgresContainer(client, containerId)
        }

        fun fromExisting(client: DockerClient, containerId: String): PostgresContainer {
            return PostgresContainer(client, containerId)
        }
    }
}

private class ExecLogger(private val logger: Logger) : ResultCallback.Adapter<Frame>() {
    override fun onNext(item: Frame) {
        logger.info(item.toString())
    }
}
