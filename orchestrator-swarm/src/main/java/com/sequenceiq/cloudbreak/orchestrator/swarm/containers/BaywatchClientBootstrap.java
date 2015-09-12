package com.sequenceiq.cloudbreak.orchestrator.swarm.containers;

import static com.sequenceiq.cloudbreak.orchestrator.containers.DockerContainer.BAYWATCH_CLIENT;
import static com.sequenceiq.cloudbreak.orchestrator.swarm.DockerClientUtil.createContainer;
import static com.sequenceiq.cloudbreak.orchestrator.swarm.DockerClientUtil.startContainer;

import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.api.model.Bind;
import com.github.dockerjava.api.model.HostConfig;
import com.google.common.base.Strings;
import com.sequenceiq.cloudbreak.orchestrator.containers.ContainerBootstrap;
import com.sequenceiq.cloudbreak.orchestrator.model.LogVolumePath;
import com.sequenceiq.cloudbreak.orchestrator.model.Node;
import com.sequenceiq.cloudbreak.orchestrator.swarm.builder.BindsBuilder;
import com.sequenceiq.cloudbreak.orchestrator.swarm.builder.HostConfigBuilder;

public class BaywatchClientBootstrap implements ContainerBootstrap {

    private static final Logger LOGGER = LoggerFactory.getLogger(BaywatchClientBootstrap.class);
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
    private final LogVolumePath logVolumePath;

    public BaywatchClientBootstrap(DockerClient docker, String gatewayAddress, String imageName, String id,
            Node node, Set<String> dataVolumes, String consulDomain, String externLocation,
            LogVolumePath logVolumePath) {
        this.docker = docker;
        this.gatewayAddress = gatewayAddress;
        this.imageName = imageName;
        this.id = id;
        this.node = node;
        this.consulDomain = consulDomain;
        this.externLocation = externLocation;
        this.dataVolumes = dataVolumes;
        this.logVolumePath = logVolumePath;
    }

    @Override
    public Boolean call() throws Exception {
        LOGGER.info("Creating Baywatch client container on: {}", node.getHostname());

        Bind[] binds = new BindsBuilder()
                .add(LOCAL_SINCEDB_LOCATION, SINCEDB_LOCATION)
                .addLog(logVolumePath, "ambari-agent", "ambari-server", "consul-watch", "consul", "kerberos").build();

        HostConfig hostConfig = new HostConfigBuilder().defaultConfig().binds(binds).build();
        String name = String.format("%s-%s", BAYWATCH_CLIENT.getName(), id);
        String baywatchIp = Strings.isNullOrEmpty(externLocation) ? gatewayAddress : externLocation;

        createContainer(docker, docker.createContainerCmd(imageName)
                .withName(name)
                .withEnv(String.format("constraint:node==%s", node.getHostname()),
                        String.format("BAYWATCH_IP=%s", baywatchIp),
                        String.format("BAYWATCH_CLUSTER_NAME=%s", BaywatchServerBootstrap.CLUSTER_NAME),
                        String.format("BAYWATCH_CLIENT_HOSTNAME=%s", node.getHostname() + consulDomain),
                        String.format("BAYWATCH_CLIENT_PRIVATE_IP=%s", node.getPrivateIp()))
                .withHostConfig(hostConfig), node.getHostname());

        startContainer(docker, name);

        return true;

    }

    @Override
    public String toString() {
        return "BaywatchClientBootstrap{"
                + "imageName='" + imageName + '\''
                + ", nodeName='" + node.getHostname() + '\''
                + '}';
    }
}
