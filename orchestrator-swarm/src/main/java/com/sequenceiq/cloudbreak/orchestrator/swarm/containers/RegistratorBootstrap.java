package com.sequenceiq.cloudbreak.orchestrator.swarm.containers;

import static com.sequenceiq.cloudbreak.orchestrator.DockerContainer.REGISTRATOR;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.api.model.Bind;
import com.github.dockerjava.api.model.HostConfig;
import com.sequenceiq.cloudbreak.orchestrator.containers.ContainerBootstrap;
import com.sequenceiq.cloudbreak.orchestrator.swarm.DockerClientUtil;
import com.sequenceiq.cloudbreak.orchestrator.swarm.builder.BindsBuilder;
import com.sequenceiq.cloudbreak.orchestrator.swarm.builder.HostConfigBuilder;

public class RegistratorBootstrap implements ContainerBootstrap {
    private static final Logger LOGGER = LoggerFactory.getLogger(RegistratorBootstrap.class);

    private static final int PORT = 9999;
    private final DockerClient docker;
    private final String nodeName;
    private final String privateIp;
    private final String imageName;
    private final DockerClientUtil dockerClientUtil;

    public RegistratorBootstrap(DockerClient docker, String imageName, String nodeName, String privateIp, DockerClientUtil dockerClientUtil) {
        this.docker = docker;
        this.nodeName = nodeName;
        this.privateIp = privateIp;
        this.imageName = imageName;
        this.dockerClientUtil = dockerClientUtil;
    }

    @Override
    public Boolean call() throws Exception {
        LOGGER.info(String.format("Registrator starting on %s node with %s private ip", nodeName, privateIp));

        Bind[] binds = new BindsBuilder()
                .addDockerSocket("/tmp/docker.sock").build();

        HostConfig hostConfig =  new HostConfigBuilder().privileged().alwaysRestart().expose(PORT).binds(binds).build();

        String containerId = dockerClientUtil.createContainer(docker, docker.createContainerCmd(imageName)
                .withEnv(String.format("constraint:node==%s", nodeName))
                .withHostConfig(hostConfig)
                .withName(REGISTRATOR.getName())
                .withCmd(String.format("consul://%s:8500", privateIp)));
        dockerClientUtil.startContainer(docker, docker.startContainerCmd(containerId));
        LOGGER.info(String.format("Registrator started successfully on node %s.", nodeName));
        return true;
    }

}
