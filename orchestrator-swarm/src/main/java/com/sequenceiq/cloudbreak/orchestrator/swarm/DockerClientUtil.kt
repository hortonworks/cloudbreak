package com.sequenceiq.cloudbreak.orchestrator.swarm

import org.slf4j.Logger
import org.slf4j.LoggerFactory

import com.github.dockerjava.api.DockerClient
import com.github.dockerjava.api.NotFoundException
import com.github.dockerjava.api.NotModifiedException
import com.github.dockerjava.api.command.CreateContainerCmd
import com.github.dockerjava.api.command.InspectContainerResponse
import com.sequenceiq.cloudbreak.orchestrator.exception.CloudbreakOrchestratorFailedException


object DockerClientUtil {

    private val LOGGER = LoggerFactory.getLogger(DockerClientUtil::class.java)

    @Throws(Exception::class)
    fun createContainer(client: DockerClient, cmd: CreateContainerCmd, node: String) {
        val name = cmd.name
        try {
            val inspectResponse = inspect(client, name)
            if (inspectResponse != null && inspectResponse.id != null && !isContainerRunning(inspectResponse)) {
                remove(client, inspectResponse, name, node)
            }
        } catch (ex: NotFoundException) {
            create(cmd, node, name)
        }

    }

    @Throws(Exception::class)
    fun startContainer(client: DockerClient, name: String) {
        try {
            start(client, name)
            val inspectResponse = inspect(client, name)
            if (inspectResponse == null || !isContainerRunning(inspectResponse)) {
                LOGGER.warn("Container {} failed to start! details: {}.", name, inspectResponse)
                throw CloudbreakOrchestratorFailedException(String.format("Container %s failed to start! ", name))
            }
        } catch (e: NotModifiedException) {
            LOGGER.info("Container {} is already running.", name)
        }

    }

    @Throws(CloudbreakOrchestratorFailedException::class)
    fun remove(client: DockerClient, name: String, node: String) {
        try {
            val inspectResponse = inspect(client, name)
            if (inspectResponse != null && inspectResponse.id != null && isContainerRunning(inspectResponse)) {
                LOGGER.warn("Container {} is still running on node: {}! Trying to remove it again.", name, node)
                remove(client, inspectResponse, name, node)
            }
            throw CloudbreakOrchestratorFailedException(String.format("Container {} is still running on node: {}!", name, node))
        } catch (ex: NotFoundException) {
            LOGGER.info("Container '{}' has already been deleted from node '{}'.", name, node)
        }

    }

    private fun start(client: DockerClient, name: String) {
        val start = System.currentTimeMillis()
        client.startContainerCmd(name).exec()
        LOGGER.info("Container {} start command took {} ms", name, System.currentTimeMillis() - start)
    }

    private fun inspect(client: DockerClient, name: String): InspectContainerResponse? {
        val start = System.currentTimeMillis()
        val inspectResponse = client.inspectContainerCmd(name).exec()
        LOGGER.info("Container {} inspect command took {} ms", name, System.currentTimeMillis() - start)
        return inspectResponse
    }

    private fun remove(client: DockerClient, inspectResponse: InspectContainerResponse, name: String, node: String) {
        LOGGER.warn("Container {} already exists, it will be removed! node: {}", name, node)
        val start = System.currentTimeMillis()
        client.removeContainerCmd(inspectResponse.id).withForce(true).exec()
        LOGGER.info("Container {} remove command took {} ms", name, System.currentTimeMillis() - start)
    }

    private fun create(cmd: CreateContainerCmd, node: String, name: String) {
        LOGGER.info("Creating container {} on node {}", name, node)
        val start = System.currentTimeMillis()
        cmd.exec()
        LOGGER.info("Container {} create command took {} ms", name, System.currentTimeMillis() - start)
    }

    private fun isContainerRunning(inspect: InspectContainerResponse): Boolean {
        return inspect.state.isRunning
    }

}
