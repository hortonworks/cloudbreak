package com.sequenceiq.cloudbreak.orchestrator.swarm.containers;

import static com.sequenceiq.cloudbreak.orchestrator.DockerContainer.REGISTRATOR;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.api.model.Bind;
import com.github.dockerjava.api.model.ExposedPort;
import com.github.dockerjava.api.model.HostConfig;
import com.github.dockerjava.api.model.PortBinding;
import com.github.dockerjava.api.model.Ports;
import com.github.dockerjava.api.model.RestartPolicy;
import com.github.dockerjava.api.model.Volume;
import com.sequenceiq.cloudbreak.orchestrator.containers.ContainerBootstrap;
import com.sequenceiq.cloudbreak.orchestrator.swarm.DockerClientUtil;

public class RegistratorBootstrap implements ContainerBootstrap {
    private static final Logger LOGGER = LoggerFactory.getLogger(RegistratorBootstrap.class);

    private static final int PORT = 9999;
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
        LOGGER.info(String.format("Registrator starting on %s node with %s private ip", nodeName, privateIp));
        HostConfig hostConfig = new HostConfig();
        hostConfig.setPrivileged(true);
        hostConfig.setRestartPolicy(RestartPolicy.alwaysRestart());
        Ports ports = new Ports();
        ports.add(new PortBinding(new Ports.Binding(PORT), new ExposedPort(PORT)));
        hostConfig.setPortBindings(ports);
        String containerId = DockerClientUtil.createContainer(docker, docker.createContainerCmd(imageName)
                .withEnv(String.format("constraint:node==%s", nodeName))
                .withHostConfig(hostConfig)
                .withName(REGISTRATOR.getName())
                .withCmd(String.format("consul://%s:8500", privateIp)));
        DockerClientUtil.startContainer(docker, docker.startContainerCmd(containerId)
                .withBinds(new Bind("/var/run/docker.sock", new Volume("/tmp/docker.sock")))
                .withRestartPolicy(RestartPolicy.alwaysRestart())
                .withPortBindings(new PortBinding(new Ports.Binding("0.0.0.0", PORT), new ExposedPort(PORT))));
        LOGGER.info(String.format("Registrator started successfully on node %s.", nodeName));
        return true;
    }
}
