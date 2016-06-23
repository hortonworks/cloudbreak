package com.sequenceiq.cloudbreak.orchestrator.swarm.containers


import org.slf4j.Logger
import org.slf4j.LoggerFactory

import com.github.dockerjava.api.DockerClient
import com.github.dockerjava.api.command.CreateContainerCmd
import com.sequenceiq.cloudbreak.orchestrator.OrchestratorBootstrap
import com.sequenceiq.cloudbreak.orchestrator.swarm.DockerClientUtil

class SwarmOrchestratorBootstrap(private val dockerClient: DockerClient, private val nodeName: String, private val createCmd: CreateContainerCmd) : OrchestratorBootstrap {

    @Throws(Exception::class)
    override fun call(): Boolean? {
        val imageName = createCmd.image
        val containerName = createCmd.name
        LOGGER.info("Creating container with name: '{}' from image: '{}' on: '{}'", containerName, imageName, nodeName)
        DockerClientUtil.createContainer(dockerClient, createCmd, nodeName)
        LOGGER.info("Starting container with name: '{}' from image: '{}' on: '{}'", containerName, imageName, nodeName)
        DockerClientUtil.startContainer(dockerClient, containerName)
        return true
    }

    companion object {
        private val LOGGER = LoggerFactory.getLogger(SwarmOrchestratorBootstrap::class.java)
    }
}
