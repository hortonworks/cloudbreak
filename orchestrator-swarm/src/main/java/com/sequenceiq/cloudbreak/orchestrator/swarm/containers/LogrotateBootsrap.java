package com.sequenceiq.cloudbreak.orchestrator.swarm.containers;

import static com.sequenceiq.cloudbreak.orchestrator.DockerContainer.LOGROTATE;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.api.model.Bind;
import com.github.dockerjava.api.model.HostConfig;
import com.sequenceiq.cloudbreak.orchestrator.containers.ContainerBootstrap;
import com.sequenceiq.cloudbreak.orchestrator.swarm.DockerClientUtil;
import com.sequenceiq.cloudbreak.orchestrator.swarm.builder.BindsBuilder;
import com.sequenceiq.cloudbreak.orchestrator.swarm.builder.HostConfigBuilder;

public class LogrotateBootsrap implements ContainerBootstrap {

    private static final Logger LOGGER = LoggerFactory.getLogger(LogrotateBootsrap.class);

    private final DockerClient docker;
    private final String id;
    private final String imageName;
    private final String nodeName;
    private final DockerClientUtil dockerClientUtil;

    public LogrotateBootsrap(DockerClient docker, String imageName, String nodeName, String id, DockerClientUtil dockerClientUtil) {
        this.docker = docker;
        this.id = id;
        this.imageName = imageName;
        this.nodeName = nodeName;
        this.dockerClientUtil = dockerClientUtil;
    }

    @Override
    public Boolean call() throws Exception {
        LOGGER.info("Creating Logrotate container.");
        Bind[] binds = new BindsBuilder()
                .add("/var/lib/docker/containers").build();

        HostConfig hostConfig = new HostConfigBuilder().defaultConfig().binds(binds).build();
        try {
            String containerId = dockerClientUtil.createContainer(docker, docker.createContainerCmd(imageName)
                    .withEnv(String.format("constraint:node==%s", nodeName))
                    .withHostConfig(hostConfig)
                    .withName(String.format("%s-%s", LOGROTATE.getName(), id)));
            dockerClientUtil.startContainer(docker, docker.startContainerCmd(containerId));
            LOGGER.info("Logrotate container started successfully");
        } catch (Exception ex) {
            LOGGER.info("Logrotate container failed to start on node %s.");
            throw ex;
        }
        return true;
    }
}
