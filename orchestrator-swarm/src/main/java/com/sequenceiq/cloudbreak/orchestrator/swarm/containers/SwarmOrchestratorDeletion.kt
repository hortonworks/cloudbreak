package com.sequenceiq.cloudbreak.orchestrator.swarm.containers

import org.slf4j.Logger
import org.slf4j.LoggerFactory

import com.github.dockerjava.api.DockerClient
import com.sequenceiq.cloudbreak.orchestrator.OrchestratorBootstrap
import com.sequenceiq.cloudbreak.orchestrator.swarm.DockerClientUtil

class SwarmOrchestratorDeletion(private val dockerClient: DockerClient, private val nodeName: String, private val containerName: String) : OrchestratorBootstrap {

    @Throws(Exception::class)
    override fun call(): Boolean? {
        LOGGER.info("Deleting container with name: '{}' from host: '{}'", containerName, nodeName)
        DockerClientUtil.remove(dockerClient, containerName, nodeName)
        return true
    }

    companion object {
        private val LOGGER = LoggerFactory.getLogger(SwarmOrchestratorDeletion::class.java)
    }
}
