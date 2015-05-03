package com.sequenceiq.cloudbreak.orcestrator.swarm.containers;

import static com.sequenceiq.cloudbreak.orcestrator.DockerContainer.CONSUL_WATCH;

import java.util.concurrent.Callable;

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
import com.sequenceiq.cloudbreak.orcestrator.swarm.DockerClientUtil;

public class ConsulWatchBootstrap implements Callable<Boolean> {
    private static final Logger LOGGER = LoggerFactory.getLogger(ConsulWatchBootstrap.class);

    private static final int PORT = 49990;
    private final DockerClient docker;
    private final String id;
    private final String imageName;

    public ConsulWatchBootstrap(DockerClient docker, String imageName, String id) {
        this.docker = docker;
        this.id = id;
        this.imageName = imageName;
    }

    @Override
    public Boolean call() throws Exception {
        LOGGER.info("Creating Consul watch container.");
        HostConfig hostConfig = new HostConfig();
        hostConfig.setPrivileged(true);
        hostConfig.setRestartPolicy(RestartPolicy.alwaysRestart());

        Ports ports = new Ports();
        ports.add(new PortBinding(new Ports.Binding(PORT), new ExposedPort(PORT)));
        hostConfig.setPortBindings(ports);

        try {
            String containerId = DockerClientUtil.createContainer(docker, docker.createContainerCmd(imageName)
                    .withHostConfig(hostConfig)
                    .withName(String.format("%s-%s", CONSUL_WATCH.getName(), id))
                    .withCmd("consul://127.0.0.1:8500"));
            DockerClientUtil.startContainer(docker, docker.startContainerCmd(containerId)
                    .withPortBindings(new PortBinding(new Ports.Binding("0.0.0.0", PORT), new ExposedPort(PORT)))
                    .withRestartPolicy(RestartPolicy.alwaysRestart())
                    .withBinds(new Bind("/var/run/docker.sock", new Volume("/var/run/docker.sock"))));
            LOGGER.info("Consul watch container started successfully");
            return true;
        } catch (Exception ex) {
            LOGGER.info("Consul watch container failed to start on node %s.");
            throw ex;
        }
    }
}
