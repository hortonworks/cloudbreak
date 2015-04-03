package com.sequenceiq.cloudbreak.core.flow.containers;

import static com.sequenceiq.cloudbreak.service.cluster.flow.DockerContainer.REGISTRATOR;

import java.util.concurrent.Callable;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.api.command.CreateContainerResponse;
import com.github.dockerjava.api.model.Bind;
import com.github.dockerjava.api.model.ExposedPort;
import com.github.dockerjava.api.model.HostConfig;
import com.github.dockerjava.api.model.PortBinding;
import com.github.dockerjava.api.model.Ports;
import com.github.dockerjava.api.model.RestartPolicy;
import com.github.dockerjava.api.model.Volume;

public class RegistratorBootstrap implements Callable<Boolean> {
    private static final Logger LOGGER = LoggerFactory.getLogger(RegistratorBootstrap.class);

    private static final int PORT = 9999;
    private final DockerClient docker;
    private final String longName;
    private final String privateIp;
    private final String containerName;

    public RegistratorBootstrap(DockerClient docker, String containerName, String longName, String privateIp) {
        this.docker = docker;
        this.longName = longName;
        this.privateIp = privateIp;
        this.containerName = containerName;
    }

    @Override
    public Boolean call() throws Exception {
        LOGGER.info(String.format("Registrator starting on %s node with %s private ip", longName, privateIp));
        HostConfig hostConfig = new HostConfig();
        hostConfig.setPrivileged(true);
        hostConfig.setRestartPolicy(RestartPolicy.alwaysRestart());
        Ports ports = new Ports();
        ports.add(new PortBinding(new Ports.Binding(PORT), new ExposedPort(PORT)));
        hostConfig.setPortBindings(ports);
        CreateContainerResponse response = docker.createContainerCmd(containerName)
                .withEnv(String.format("constraint:node==%s", longName))
                .withHostConfig(hostConfig)
                .withName(REGISTRATOR.getName())
                .withCmd(String.format("consul://%s:8500", privateIp))
                .exec();
        docker.startContainerCmd(response.getId())
                .withBinds(new Bind("/var/run/docker.sock", new Volume("/tmp/docker.sock")))
                .withRestartPolicy(RestartPolicy.alwaysRestart())
                .withPortBindings(new PortBinding(new Ports.Binding("0.0.0.0", PORT), new ExposedPort(PORT)))
                .exec();
        LOGGER.info(String.format("Registrator start was success on %s node with %s private ip", longName, privateIp));
        return true;
    }
}
