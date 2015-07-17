package com.sequenceiq.cloudbreak.orchestrator.swarm.containers;

import static com.sequenceiq.cloudbreak.orchestrator.containers.DockerContainer.AMBARI_SERVER;
import static com.sequenceiq.cloudbreak.orchestrator.swarm.DockerClientUtil.createContainer;
import static com.sequenceiq.cloudbreak.orchestrator.swarm.DockerClientUtil.startContainer;

import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.api.model.Bind;
import com.github.dockerjava.api.model.HostConfig;
import com.sequenceiq.cloudbreak.orchestrator.containers.ContainerBootstrap;
import com.sequenceiq.cloudbreak.orchestrator.model.LogVolumePath;
import com.sequenceiq.cloudbreak.orchestrator.swarm.builder.BindsBuilder;
import com.sequenceiq.cloudbreak.orchestrator.swarm.builder.HostConfigBuilder;

public class AmbariServerBootstrap implements ContainerBootstrap {

    private static final Logger LOGGER = LoggerFactory.getLogger(AmbariServerBootstrap.class);

    private static final int PORT = 8080;
    private final DockerClient docker;
    private final String imageName;
    private final String cloudPlatform;
    private final String nodeName;
    private final Set<String> dataVolumes;
    private final LogVolumePath logVolumePath;

    public AmbariServerBootstrap(DockerClient docker, String imageName, String nodeName, Set<String> dataVolumes,
            String cloudPlatform, LogVolumePath logVolumePath) {
        this.docker = docker;
        this.imageName = imageName;
        this.cloudPlatform = cloudPlatform;
        this.nodeName = nodeName;
        this.dataVolumes = dataVolumes;
        this.logVolumePath = logVolumePath;
    }

    @Override
    public Boolean call() throws Exception {
        LOGGER.info("Creating Ambari server container on: {}", nodeName);

        Bind[] binds = new BindsBuilder().addLog(logVolumePath).build();
        HostConfig hostConfig = new HostConfigBuilder().defaultConfig().expose(PORT).binds(binds).build();

        String name = AMBARI_SERVER.getName();
        createContainer(docker, docker.createContainerCmd(imageName)
                .withHostConfig(hostConfig)
                .withName(name)
                .withEnv(String.format("constraint:node==%s", nodeName),
                        String.format("POSTGRES_DB=localhost"),
                        String.format("CLOUD_PLATFORM=%s", cloudPlatform),
                        String.format("SERVICE_NAME=%s", "ambari-8080"))
                .withCmd("/start-server"));

        startContainer(docker, name);

        return true;
    }

}
