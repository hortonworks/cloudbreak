package com.sequenceiq.cloudbreak.orchestrator.swarm.containers;


import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.api.command.CreateContainerCmd;
import com.sequenceiq.cloudbreak.orchestrator.OrchestratorBootstrap;
import com.sequenceiq.cloudbreak.orchestrator.swarm.DockerClientUtil;

public class SwarmOrchestratorBootstrap implements OrchestratorBootstrap {
    private static final Logger LOGGER = LoggerFactory.getLogger(SwarmOrchestratorBootstrap.class);

    private final DockerClient dockerClient;
    private final String nodeName;
    private final CreateContainerCmd createCmd;

    public SwarmOrchestratorBootstrap(DockerClient dockerClient, String nodeName, CreateContainerCmd createCmd) {
        this.dockerClient = dockerClient;
        this.nodeName = nodeName;
        this.createCmd = createCmd;
    }

    @Override
    public Boolean call() throws Exception {
        String imageName = createCmd.getImage();
        String containerName = createCmd.getName();
        LOGGER.info("Creating container with name: '{}' from image: '{}' on: '{}'", containerName, imageName, nodeName);
        DockerClientUtil.createContainer(dockerClient, createCmd, nodeName);
        LOGGER.info("Starting container with name: '{}' from image: '{}' on: '{}'", containerName, imageName, nodeName);
        DockerClientUtil.startContainer(dockerClient, containerName);
        return true;
    }
}
