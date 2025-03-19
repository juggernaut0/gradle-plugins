package dev.twarner.gradle

import com.fasterxml.jackson.databind.json.JsonMapper
import com.fasterxml.jackson.databind.node.ObjectNode
import com.github.dockerjava.api.DockerClient
import com.github.dockerjava.api.model.AuthConfig
import com.github.dockerjava.core.DefaultDockerClientConfig
import com.github.dockerjava.core.DockerClientImpl
import com.github.dockerjava.zerodep.ZerodepDockerHttpClient
import org.gradle.api.logging.Logging
import org.gradle.process.ExecOperations
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.util.*
import javax.inject.Inject
import kotlin.io.path.Path
import kotlin.io.path.exists

open class DockerClientProvider @Inject constructor(private val execOperations: ExecOperations) {
    private val log = Logging.getLogger(DockerClientProvider::class.java)

    private val json = JsonMapper.builder().build()
    private val configFile: ObjectNode by lazy {
        val userHome = Path(System.getProperty("user.home"))
        val configFile = userHome.resolve(".docker/config.json")
        if (configFile.exists()) {
            json.readTree(configFile.toFile()) as ObjectNode
        } else {
            json.createObjectNode()
        }
    }

    fun newDockerClient(): DockerClient {
        val config = DefaultDockerClientConfig.createDefaultConfigBuilder().build()
        getRegistryAuth(AuthConfig.DEFAULT_SERVER_ADDRESS)?.also {
            log.info("Adding auth config for ${it.registryAddress}")
            config.dockerConfig.auths[it.registryAddress] = it
        }
        val httpClient = ZerodepDockerHttpClient.Builder().dockerHost(config.dockerHost).build()
        return DockerClientImpl.getInstance(config, httpClient)
    }

    private fun getRegistryAuth(host: String): AuthConfig? {
        val authsNode = configFile["auths"] as? ObjectNode
        val authNode = authsNode?.get(host)
        if (authNode != null && authNode.hasNonNull("auth")) {
            val decoded = String(Base64.getDecoder().decode(authNode["auth"].asText()))
            val (username, password) = decoded.split(":", limit = 2)
            return AuthConfig().withRegistryAddress(host).withUsername(username).withPassword(password)
        }

        val credsStore = configFile["credsStore"]?.takeUnless { it.isNull }?.asText()
        if (!credsStore.isNullOrBlank()) {
            log.info("Using creds store $credsStore for host $host")
            val sOut = ByteArrayOutputStream()
            execOperations.exec {
                setCommandLine("docker-credential-$credsStore", "get")
                standardOutput = sOut
                isIgnoreExitValue = true
                standardInput = ByteArrayInputStream(host.encodeToByteArray())
            }
            val creds = json.readTree(sOut.toByteArray()) as? ObjectNode ?: return null
            val username = creds["Username"].asText()
            val password = creds["Secret"].asText()
            return AuthConfig().withRegistryAddress(host).withUsername(username).withPassword(password)
        }

        return null
    }
}
