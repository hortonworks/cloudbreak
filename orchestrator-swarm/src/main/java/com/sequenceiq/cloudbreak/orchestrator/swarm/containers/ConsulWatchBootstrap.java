package com.sequenceiq.cloudbreak.orchestrator.swarm.containers;

import static com.sequenceiq.cloudbreak.orchestrator.containers.DockerContainer.CONSUL_WATCH;
import static com.sequenceiq.cloudbreak.orchestrator.swarm.DockerClientUtil.createContainer;
import static com.sequenceiq.cloudbreak.orchestrator.swarm.DockerClientUtil.startContainer;
import static java.lang.String.format;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.api.model.Bind;
import com.github.dockerjava.api.model.HostConfig;
import com.sequenceiq.cloudbreak.orchestrator.containers.ContainerBootstrap;
import com.sequenceiq.cloudbreak.orchestrator.model.LogVolumePath;
import com.sequenceiq.cloudbreak.orchestrator.model.Node;
import com.sequenceiq.cloudbreak.orchestrator.swarm.builder.BindsBuilder;
import com.sequenceiq.cloudbreak.orchestrator.swarm.builder.HostConfigBuilder;

public class ConsulWatchBootstrap implements ContainerBootstrap {
    private static final Logger LOGGER = LoggerFactory.getLogger(ConsulWatchBootstrap.class);

    private final DockerClient docker;
    private final String id;
    private final String imageName;
    private final Node node;
    private final LogVolumePath logVolumePath;

    public ConsulWatchBootstrap(DockerClient docker, String imageName, Node node, String id,
            LogVolumePath logVolumePath) {
        this.docker = docker;
        this.id = id;
        this.imageName = imageName;
        this.logVolumePath = logVolumePath;
        this.node = new Node(node);
    }

    @Override
    public Boolean call() throws Exception {
        LOGGER.info("Creating Consul watch container on: {}", node.getHostname());

        Bind[] binds = new BindsBuilder()
                .addDockerSocket()
                .add(logVolumePath.getHostPath() + "/consul-watch",
                        logVolumePath.getContainerPath() + "/consul-watch").build();

        HostConfig hostConfig = new HostConfigBuilder().defaultConfig().binds(binds).build();
        String name = format("%s-%s", CONSUL_WATCH.getName(), id);
        String ip = node.getPrivateIp();

        long createStartTime = System.currentTimeMillis();

        createContainer(docker, docker.createContainerCmd(imageName)
                .withHostConfig(hostConfig)
                .withName(name)
                .withEnv(format("constraint:node==%s", node.getHostname()), format("CONSUL_HOST=%s", ip))
                .withCmd(format("consul://%s:8500", ip)));

        long elapsedTime = System.currentTimeMillis() - createStartTime;

        LOGGER.info("Consul watch container created on: {}, within {} ms", node.getHostname(), elapsedTime);

        long startTime = System.currentTimeMillis();
        startContainer(docker, name);
        elapsedTime = System.currentTimeMillis() - startTime;

        LOGGER.info("Consul watch container started on: {}  within {} ms", node.getHostname(), elapsedTime);

        return true;
    }

    @Override
    public String toString() {
        return "ConsulWatchBootstrap{"
                + "imageName='" + imageName + '\''
                + ", nodeName='" + node.getHostname() + '\''
                + '}';
    }

}
