package com.sequenceiq.cloudbreak.orchestrator.swarm.containers;

import static com.sequenceiq.cloudbreak.orchestrator.containers.DockerContainer.REGISTRATOR;
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

public class RegistratorBootstrap implements ContainerBootstrap {
    private static final Logger LOGGER = LoggerFactory.getLogger(RegistratorBootstrap.class);

    private final DockerClient docker;
    private final String nodeName;
    private final String privateIp;
    private final String imageName;

    public RegistratorBootstrap(DockerClient docker, String imageName, String nodeName, String privateIp) {
        this.docker = docker;
        this.nodeName = nodeName;
        this.privateIp = privateIp;
        this.imageName = imageName;
    }

    @Override
    public Boolean call() throws Exception {
        LOGGER.info("Registrator starting on {} node with {} private ip", nodeName, privateIp);

        Bind[] binds = new BindsBuilder()
                .addDockerSocket("/tmp/docker.sock").build();

        HostConfig hostConfig = new HostConfigBuilder().defaultConfig().binds(binds).build();

        String name = REGISTRATOR.getName();
        createContainer(docker, docker.createContainerCmd(imageName)
                .withEnv(String.format("constraint:node==%s", nodeName))
                .withHostConfig(hostConfig)
                .withName(name)
                .withCmd(String.format("consul://%s:8500", privateIp)), nodeName);

        startContainer(docker, name);
        LOGGER.info(String.format("Registrator started successfully on node %s.", nodeName));
        return true;
    }

    @Override
    public String toString() {
        return "RegistratorBootstrap{"
                + "imageName='" + imageName + '\''
                + ", nodeName='" + nodeName + '\''
                + '}';
    }

}
