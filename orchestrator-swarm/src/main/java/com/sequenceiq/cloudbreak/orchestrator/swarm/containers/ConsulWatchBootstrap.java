package com.sequenceiq.cloudbreak.orchestrator.swarm.containers;

import static com.sequenceiq.cloudbreak.orchestrator.DockerContainer.CONSUL_WATCH;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.api.model.Bind;
import com.github.dockerjava.api.model.HostConfig;
import com.sequenceiq.cloudbreak.orchestrator.containers.ContainerBootstrap;
import com.sequenceiq.cloudbreak.orchestrator.swarm.DockerClientUtil;
import com.sequenceiq.cloudbreak.orchestrator.swarm.builder.BindsBuilder;
import com.sequenceiq.cloudbreak.orchestrator.swarm.builder.HostConfigBuilder;

public class ConsulWatchBootstrap implements ContainerBootstrap {
    private static final Logger LOGGER = LoggerFactory.getLogger(ConsulWatchBootstrap.class);

    private final DockerClient docker;
    private final String id;
    private final String imageName;
    private final String nodeName;
    private final DockerClientUtil dockerClientUtil;

    public ConsulWatchBootstrap(DockerClient docker, String imageName, String nodeName, String id, DockerClientUtil dockerClientUtil) {
        this.docker = docker;
        this.id = id;
        this.imageName = imageName;
        this.nodeName = nodeName;
        this.dockerClientUtil = dockerClientUtil;
    }

    @Override
    public Boolean call() throws Exception {
        LOGGER.info("Creating Consul watch container.");

        Bind[] binds = new BindsBuilder()
                .addDockerSocket()
                .add("/hadoopfs/fs1/logs/consul-watch", "/var/log/consul-watch").build();

        HostConfig hostConfig = new HostConfigBuilder().defaultConfig().binds(binds).build();

        try {
            String containerId = dockerClientUtil.createContainer(docker, docker.createContainerCmd(imageName)
                    .withHostConfig(hostConfig)
                    .withEnv(String.format("constraint:node==%s", nodeName))
                    .withName(String.format("%s-%s", CONSUL_WATCH.getName(), id))
                    .withCmd("consul://127.0.0.1:8500"));
            dockerClientUtil.startContainer(docker, docker.startContainerCmd(containerId));
            LOGGER.info("Consul watch container started successfully");
        } catch (Exception ex) {
            LOGGER.info("Consul watch container failed to start on node %s.");
            throw ex;
        }
        return true;
    }
}
