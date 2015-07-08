package com.sequenceiq.cloudbreak.orchestrator.swarm.containers;

import static com.sequenceiq.cloudbreak.orchestrator.DockerContainer.AMBARI_SERVER;

import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.api.model.Bind;
import com.github.dockerjava.api.model.HostConfig;
import com.sequenceiq.cloudbreak.orchestrator.containers.ContainerBootstrap;
import com.sequenceiq.cloudbreak.orchestrator.swarm.DockerClientUtil;
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
    private final DockerClientUtil dockerClientUtil;

    public AmbariServerBootstrap(DockerClient docker, String imageName, String nodeName, Set<String> dataVolumes,
            String cloudPlatform, DockerClientUtil dockerClientUtil) {
        this.docker = docker;
        this.imageName = imageName;
        this.cloudPlatform = cloudPlatform;
        this.nodeName = nodeName;
        this.dataVolumes = dataVolumes;
        this.dockerClientUtil = dockerClientUtil;
    }

    @Override
    public Boolean call() throws Exception {

        Bind[] binds = new BindsBuilder().addLog().build();
        HostConfig hostConfig = new HostConfigBuilder().defaultConfig().expose(PORT).binds(binds).build();

        String containerId = dockerClientUtil.createContainer(docker, docker.createContainerCmd(imageName)
                .withHostConfig(hostConfig)
                .withEnv(String.format("constraint:node==%s", nodeName),
                        String.format("POSTGRES_DB=localhost"),
                        String.format("CLOUD_PLATFORM=%s", cloudPlatform),
                        String.format("SERVICE_NAME=%s", "ambari-8080"))
                .withName(AMBARI_SERVER.getName())
                .withCmd("/start-server"));

        dockerClientUtil.startContainer(docker, docker.startContainerCmd(containerId));
        LOGGER.info("Ambari server started successfully");
        return true;
    }

}
