package com.sequenceiq.cloudbreak.orchestrator.swarm.containers;

import static com.sequenceiq.cloudbreak.orchestrator.DockerContainer.BAYWATCH_CLIENT;

import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.api.model.Bind;
import com.github.dockerjava.api.model.HostConfig;
import com.google.common.base.Strings;
import com.sequenceiq.cloudbreak.orchestrator.Node;
import com.sequenceiq.cloudbreak.orchestrator.containers.ContainerBootstrap;
import com.sequenceiq.cloudbreak.orchestrator.swarm.DockerClientUtil;
import com.sequenceiq.cloudbreak.orchestrator.swarm.builder.BindsBuilder;
import com.sequenceiq.cloudbreak.orchestrator.swarm.builder.HostConfigBuilder;

public class BaywatchClientBootstrap implements ContainerBootstrap {

    private static final Logger LOGGER = LoggerFactory.getLogger(BaywatchClientBootstrap.class);
    private static final String DOCKER_LOG_LOCATION = "/var/log/containers";
    private static final String SINCEDB_LOCATION = "/sincedb";
    private static final String LOCAL_SINCEDB_LOCATION = "/log-sincedb";

    private final DockerClient docker;
    private final String gatewayAddress;
    private final String imageName;
    private final String id;
    private final String externLocation;
    private final Node node;
    private final String consulDomain;
    private final Set<String> dataVolumes;
    private DockerClientUtil dockerClientUtil;

    public BaywatchClientBootstrap(DockerClient docker, String gatewayAddress, String imageName, String id,
            Node node, Set<String> dataVolumes, String consulDomain, String externLocation, DockerClientUtil dockerClientUtil) {
        this.docker = docker;
        this.gatewayAddress = gatewayAddress;
        this.imageName = imageName;
        this.id = id;
        this.node = node;
        this.consulDomain = consulDomain;
        this.externLocation = externLocation;
        this.dataVolumes = dataVolumes;
        this.dockerClientUtil = dockerClientUtil;
    }

    @Override
    public Boolean call() throws Exception {
        LOGGER.info("Creating Baywatch client container.");

        Bind[] binds = new BindsBuilder()
                .add(LOCAL_SINCEDB_LOCATION, SINCEDB_LOCATION)
                .addLog("ambari-agent", "ambari-server", "consul-watch", "consul").build();

        HostConfig hostConfig = new HostConfigBuilder().defaultConfig().binds(binds).build();
        try {
            String baywatchIp = Strings.isNullOrEmpty(externLocation) ? gatewayAddress : externLocation;
            String containerId = dockerClientUtil.createContainer(docker, docker.createContainerCmd(imageName)
                    .withName(String.format("%s-%s", BAYWATCH_CLIENT.getName(), id))
                    .withEnv(String.format("constraint:node==%s", node.getHostname()),
                            String.format("BAYWATCH_IP=%s", baywatchIp),
                            String.format("BAYWATCH_CLUSTER_NAME=%s", BaywatchServerBootstrap.CLUSTER_NAME),
                            String.format("BAYWATCH_CLIENT_HOSTNAME=%s", node.getHostname() + consulDomain),
                            String.format("BAYWATCH_CLIENT_PRIVATE_IP=%s", node.getPrivateIp()))
                    .withHostConfig(hostConfig));

            dockerClientUtil.startContainer(docker, docker.startContainerCmd(containerId));
            LOGGER.info("Baywatch client container started successfully");
            return true;
        } catch (Exception ex) {
            LOGGER.info("Baywatch client container failed to start.");
            throw ex;
        }
    }
}
