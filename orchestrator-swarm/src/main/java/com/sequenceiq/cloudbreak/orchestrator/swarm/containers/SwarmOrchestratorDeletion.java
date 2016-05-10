package com.sequenceiq.cloudbreak.orchestrator.swarm.containers;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.github.dockerjava.api.DockerClient;
import com.sequenceiq.cloudbreak.orchestrator.OrchestratorBootstrap;
import com.sequenceiq.cloudbreak.orchestrator.swarm.DockerClientUtil;

public class SwarmOrchestratorDeletion implements OrchestratorBootstrap {
    private static final Logger LOGGER = LoggerFactory.getLogger(SwarmOrchestratorDeletion.class);

    private final DockerClient dockerClient;
    private final String nodeName;
    private final String containerName;

    public SwarmOrchestratorDeletion(DockerClient dockerClient, String nodeName, String containerName) {
        this.dockerClient = dockerClient;
        this.nodeName = nodeName;
        this.containerName = containerName;
    }

    @Override
    public Boolean call() throws Exception {
        LOGGER.info("Deleting container with name: '{}' from host: '{}'", containerName, nodeName);
        DockerClientUtil.remove(dockerClient, containerName, nodeName);
        return true;
    }
}
