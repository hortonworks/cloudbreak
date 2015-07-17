package com.sequenceiq.cloudbreak.orchestrator.swarm.containers;

import static com.sequenceiq.cloudbreak.orchestrator.containers.DockerContainer.BAYWATCH_SERVER;
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


public class BaywatchServerBootstrap implements ContainerBootstrap {

    public static final String CLUSTER_NAME = "hdp-log-cluster";

    private static final Logger LOGGER = LoggerFactory.getLogger(BaywatchServerBootstrap.class);

    private static final String ES_WORK_PATH = "/es-work";
    private static final String ES_DATA_PATH = "/es-data";

    private final DockerClient docker;
    private final String imageName;
    private final String nodeName;

    public BaywatchServerBootstrap(DockerClient docker, String imageName, String nodeName) {
        this.docker = docker;
        this.imageName = imageName;
        this.nodeName = nodeName;
    }

    @Override
    public Boolean call() throws Exception {

        LOGGER.info("Creating Baywatch server container on: {}", nodeName);

        Bind[] binds = new BindsBuilder()
                .add(ES_WORK_PATH)
                .add(ES_DATA_PATH).build();

        HostConfig hostConfig = new HostConfigBuilder().defaultConfig().binds(binds).build();
        String name = BAYWATCH_SERVER.getName();

        createContainer(docker, docker.createContainerCmd(imageName)
                .withName(name)
                .withEnv(String.format("constraint:node==%s", nodeName),
                        String.format("ES_CLUSTER_NAME=%s", CLUSTER_NAME),
                        String.format("ES_DATA_PATH=%s", ES_DATA_PATH),
                        String.format("ES_WORK_PATH=%s", ES_WORK_PATH))
                .withHostConfig(hostConfig));

        startContainer(docker, name);

        return true;

    }
}
