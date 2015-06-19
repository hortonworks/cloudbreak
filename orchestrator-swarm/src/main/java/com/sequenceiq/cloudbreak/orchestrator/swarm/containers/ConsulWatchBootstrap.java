package com.sequenceiq.cloudbreak.orchestrator.swarm.containers;

import static com.sequenceiq.cloudbreak.orchestrator.DockerContainer.CONSUL_WATCH;

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

public class ConsulWatchBootstrap implements ContainerBootstrap {
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
        hostConfig.setNetworkMode("host");
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
                    .withNetworkMode("host")
                    .withRestartPolicy(RestartPolicy.alwaysRestart())
                    .withBinds(new Bind("/var/run/docker.sock", new Volume("/var/run/docker.sock")),
                            new Bind("/hadoopfs/fs1/logs/consul-watch", new Volume("/var/log/consul-watch"))));
            LOGGER.info("Consul watch container started successfully");
        } catch (Exception ex) {
            LOGGER.info("Consul watch container failed to start on node %s.");
            throw ex;
        }
        return true;
    }
}
