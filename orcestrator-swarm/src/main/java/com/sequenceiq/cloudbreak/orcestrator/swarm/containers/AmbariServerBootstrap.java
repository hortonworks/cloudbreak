package com.sequenceiq.cloudbreak.orcestrator.swarm.containers;

import static com.sequenceiq.cloudbreak.orcestrator.DockerContainer.AMBARI_SERVER;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.api.model.ExposedPort;
import com.github.dockerjava.api.model.HostConfig;
import com.github.dockerjava.api.model.PortBinding;
import com.github.dockerjava.api.model.Ports;
import com.github.dockerjava.api.model.RestartPolicy;
import com.sequenceiq.cloudbreak.orcestrator.containers.ContainerBootstrap;
import com.sequenceiq.cloudbreak.orcestrator.swarm.DockerClientUtil;

public class AmbariServerBootstrap implements ContainerBootstrap {

    private static final Logger LOGGER = LoggerFactory.getLogger(AmbariServerBootstrap.class);

    private static final int PORT = 8080;
    private final DockerClient docker;
    private final String imageName;
    private final String cloudPlatform;

    public AmbariServerBootstrap(DockerClient docker, String imageName, String cloudPlatform) {
        this.docker = docker;
        this.imageName = imageName;
        this.cloudPlatform = cloudPlatform;
    }

    @Override
    public Boolean call() throws Exception {
        HostConfig hostConfig = new HostConfig();
        hostConfig.setNetworkMode("host");
        hostConfig.setPrivileged(true);
        hostConfig.setRestartPolicy(RestartPolicy.alwaysRestart());
        Ports ports = new Ports();
        ports.add(new PortBinding(new Ports.Binding(PORT), new ExposedPort(PORT)));
        hostConfig.setPortBindings(ports);

        String containerId = DockerClientUtil.createContainer(docker, docker.createContainerCmd(imageName)
                .withHostConfig(hostConfig)
                .withExposedPorts(new ExposedPort(PORT))
                .withEnv("constraint:type==gateway",
                        String.format("POSTGRES_DB=localhost"),
                        String.format("CLOUD_PLATFORM=%s", cloudPlatform),
                        String.format("SERVICE_NAME=%s", "ambari-8080"))
                .withName(AMBARI_SERVER.getName())
                .withCmd("/start-server"));
        DockerClientUtil.startContainer(docker, docker.startContainerCmd(containerId)
                .withPortBindings(new PortBinding(new Ports.Binding("0.0.0.0", PORT), new ExposedPort(PORT)))
                .withNetworkMode("host")
                .withRestartPolicy(RestartPolicy.alwaysRestart())
                .withPrivileged(true));
        LOGGER.info("Ambari server started successfully");
        return true;
    }
}
