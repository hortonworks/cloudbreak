package com.sequenceiq.cloudbreak.orchestrator.swarm.containers;

import static com.sequenceiq.cloudbreak.orchestrator.DockerContainer.BAYWATCH_SERVER;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.api.model.Bind;
import com.github.dockerjava.api.model.HostConfig;
import com.sequenceiq.cloudbreak.orchestrator.containers.ContainerBootstrap;
import com.sequenceiq.cloudbreak.orchestrator.swarm.DockerClientUtil;
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
    private final DockerClientUtil dockerClientUtil;

    public BaywatchServerBootstrap(DockerClient docker, String imageName, String nodeName, DockerClientUtil dockerClientUtil) {
        this.docker = docker;
        this.imageName = imageName;
        this.nodeName = nodeName;
        this.dockerClientUtil = dockerClientUtil;
    }

    @Override
    public Boolean call() throws Exception {

        Bind[] binds = new BindsBuilder()
                .add(ES_WORK_PATH)
                .add(ES_DATA_PATH).build();

        HostConfig hostConfig = new HostConfigBuilder().defaultConfig().binds(binds).build();
        try {
            String containerId = dockerClientUtil.createContainer(docker, docker.createContainerCmd(imageName)
                    .withName(BAYWATCH_SERVER.getName())
                    .withEnv(String.format("constraint:node==%s", nodeName),
                            String.format("ES_CLUSTER_NAME=%s", CLUSTER_NAME),
                            String.format("ES_DATA_PATH=%s", ES_DATA_PATH),
                            String.format("ES_WORK_PATH=%s", ES_WORK_PATH))
                    .withHostConfig(hostConfig));

            dockerClientUtil.startContainer(docker, docker.startContainerCmd(containerId));
            LOGGER.info("Baywatch server container started successfully");
            return true;
        } catch (Exception ex) {
            LOGGER.info("Baywatch server container failed to start.");
            throw ex;
        }
    }
}
