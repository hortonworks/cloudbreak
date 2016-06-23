package com.sequenceiq.cloudbreak.orchestrator.swarm.containers

import com.sequenceiq.cloudbreak.orchestrator.container.DockerContainer.MUNCHAUSEN
import com.sequenceiq.cloudbreak.orchestrator.swarm.DockerClientUtil.createContainer
import com.sequenceiq.cloudbreak.orchestrator.swarm.DockerClientUtil.startContainer

import java.util.Arrays
import java.util.Date

import org.slf4j.Logger
import org.slf4j.LoggerFactory

import com.github.dockerjava.api.DockerClient
import com.github.dockerjava.api.model.Bind
import com.sequenceiq.cloudbreak.orchestrator.OrchestratorBootstrap
import com.sequenceiq.cloudbreak.orchestrator.swarm.builder.BindsBuilder

class MunchausenBootstrap(private val docker: DockerClient, private val containerName: String, private val cmd: Array<String>) : OrchestratorBootstrap {

    @Throws(Exception::class)
    override fun call(): Boolean? {

        val binds = BindsBuilder().addDockerSocket().build()

        val name = MUNCHAUSEN.name + Date().time
        createContainer(docker, docker.createContainerCmd(containerName).withName(name).withPrivileged(true).withBinds(*binds).withCmd(*cmd), name)

        startContainer(docker, name)
        LOGGER.info("Munchausen bootstrap container started.")
        return true
    }

    override fun toString(): String {
        val sb = StringBuilder("MunchausenBootstrap{")
        sb.append("docker=").append(docker)
        sb.append(", cmd=").append(Arrays.toString(cmd))
        sb.append(", containerName='").append(containerName).append('\'')
        sb.append('}')
        return sb.toString()
    }

    companion object {

        private val LOGGER = LoggerFactory.getLogger(MunchausenBootstrap::class.java)
    }
}
