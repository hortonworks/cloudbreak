package com.sequenceiq.cloudbreak.orchestrator.swarm.containers;

import static com.sequenceiq.cloudbreak.orchestrator.DockerContainer.BAYWATCH_CLIENT;

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
import com.google.common.base.Strings;
import com.sequenceiq.cloudbreak.orchestrator.Node;
import com.sequenceiq.cloudbreak.orchestrator.containers.ContainerBootstrap;
import com.sequenceiq.cloudbreak.orchestrator.swarm.DockerClientUtil;

public class BaywatchClientBootstrap implements ContainerBootstrap {

    private static final Logger LOGGER = LoggerFactory.getLogger(BaywatchClientBootstrap.class);
    private static final int PORT = 49999;
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

    public BaywatchClientBootstrap(DockerClient docker, String gatewayAddress, String imageName, String id,
            Node node, String consulDomain, String externLocation) {
        this.docker = docker;
        this.gatewayAddress = gatewayAddress;
        this.imageName = imageName;
        this.id = id;
        this.node = node;
        this.consulDomain = consulDomain;
        this.externLocation = externLocation;
    }

    @Override
    public Boolean call() throws Exception {
        LOGGER.info("Creating Baywatch client container.");
        HostConfig hostConfig = new HostConfig();
        hostConfig.setPrivileged(true);
        hostConfig.setNetworkMode("host");
        hostConfig.setRestartPolicy(RestartPolicy.alwaysRestart());
        try {
            String baywatchIp = Strings.isNullOrEmpty(externLocation) ? gatewayAddress : externLocation;
            String containerId = DockerClientUtil.createContainer(docker, docker.createContainerCmd(imageName)
                    .withName(String.format("%s-%s", BAYWATCH_CLIENT.getName(), id))
                    .withEnv(String.format("constraint:node==%s", node.getHostname()),
                            String.format("BAYWATCH_IP=%s", baywatchIp),
                            String.format("BAYWATCH_CLUSTER_NAME=%s", BaywatchServerBootstrap.CLUSTER_NAME),
                            String.format("BAYWATCH_CLIENT_HOSTNAME=%s", node.getHostname() + consulDomain),
                            String.format("BAYWATCH_CLIENT_PRIVATE_IP=%s", node.getPrivateIp()),
                            String.format("BAYWATCH_CLIENT_PUBLIC_IP=%s", node.getPublicIp()))
                            .withHostConfig(hostConfig));
            DockerClientUtil.startContainer(docker, docker.startContainerCmd(containerId)
                    .withPortBindings(new PortBinding(new Ports.Binding("0.0.0.0", PORT), new ExposedPort(PORT)))
                    .withBinds(new Bind(DOCKER_LOG_LOCATION, new Volume(DOCKER_LOG_LOCATION)),
                            new Bind(LOCAL_SINCEDB_LOCATION, new Volume(SINCEDB_LOCATION)))
                    .withNetworkMode("host")
                    .withRestartPolicy(RestartPolicy.alwaysRestart()));
            LOGGER.info("Baywatch client container started successfully");
            return true;
        } catch (Exception ex) {
            LOGGER.info("Baywatch client container failed to start.");
            throw ex;
        }
    }
}
