package com.sequenceiq.cloudbreak.orchestrator.swarm.containers;

import static com.sequenceiq.cloudbreak.orchestrator.containers.DockerContainer.LOGROTATE;
import static com.sequenceiq.cloudbreak.orchestrator.swarm.DockerClientUtil.createContainer;
import static com.sequenceiq.cloudbreak.orchestrator.swarm.DockerClientUtil.startContainer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.api.model.Bind;
import com.github.dockerjava.api.model.HostConfig;
import com.sequenceiq.cloudbreak.orchestrator.containers.ContainerBootstrap;
import com.sequenceiq.cloudbreak.orchestrator.swarm.builder.BindsBuilder;
import com.sequenceiq.cloudbreak.orchestrator.swarm.builder.HostConfigBuilder;

public class LogrotateBootsrap implements ContainerBootstrap {

    private static final Logger LOGGER = LoggerFactory.getLogger(LogrotateBootsrap.class);

    private final DockerClient docker;
    private final String id;
    private final String imageName;
    private final String nodeName;

    public LogrotateBootsrap(DockerClient docker, String imageName, String nodeName, String id) {
        this.docker = docker;
        this.id = id;
        this.imageName = imageName;
        this.nodeName = nodeName;
    }

    @Override
    public Boolean call() throws Exception {
        LOGGER.info("Creating Logrotate container on: {}", nodeName);

        Bind[] binds = new BindsBuilder()
                .add("/var/lib/docker/containers").build();

        HostConfig hostConfig = new HostConfigBuilder().defaultConfig().binds(binds).build();
        String name = String.format("%s-%s", LOGROTATE.getName(), id);

        createContainer(docker, docker.createContainerCmd(imageName)
                .withEnv(String.format("constraint:node==%s", nodeName))
                .withHostConfig(hostConfig)
                .withName(name));
        startContainer(docker, name);
        LOGGER.info("Logrotate container started successfully");

        return true;
    }

    @Override
    public String toString() {
        return "LogrotateBootsrap{"
                + "imageName='" + imageName + '\''
                + ", nodeName='" + nodeName + '\''
                + '}';
    }
}
