package com.sequenceiq.cloudbreak.orchestrator.swarm.containers;

import static com.sequenceiq.cloudbreak.orchestrator.DockerContainer.AMBARI_SERVER;

import java.util.Set;

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
import com.sequenceiq.cloudbreak.orchestrator.containers.ContainerBootstrap;
import com.sequenceiq.cloudbreak.orchestrator.swarm.DockerClientUtil;

public class AmbariServerBootstrap implements ContainerBootstrap {

    private static final Logger LOGGER = LoggerFactory.getLogger(AmbariServerBootstrap.class);

    private static final int PORT = 8080;
    private final DockerClient docker;
    private final String imageName;
    private final String cloudPlatform;
    private final String node;
    private final Set<String> dataVolumes;
    private final DockerClientUtil dockerClientUtil;

    public AmbariServerBootstrap(DockerClient docker, String imageName, String node, Set<String> dataVolumes,
            String cloudPlatform, DockerClientUtil dockerClientUtil) {
        this.docker = docker;
        this.imageName = imageName;
        this.cloudPlatform = cloudPlatform;
        this.node = node;
        this.dataVolumes = dataVolumes;
        this.dockerClientUtil = dockerClientUtil;
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

        String containerId = dockerClientUtil.createContainer(docker, docker.createContainerCmd(imageName)
                .withHostConfig(hostConfig)
                .withExposedPorts(new ExposedPort(PORT))
                .withEnv(String.format("constraint:node==%s", node),
                        String.format("POSTGRES_DB=localhost"),
                        String.format("CLOUD_PLATFORM=%s", cloudPlatform),
                        String.format("SERVICE_NAME=%s", "ambari-8080"))
                .withName(AMBARI_SERVER.getName())
                .withCmd("/start-server"));
        dockerClientUtil.startContainer(docker, docker.startContainerCmd(containerId)
                .withPortBindings(new PortBinding(new Ports.Binding("0.0.0.0", PORT), new ExposedPort(PORT)))
                .withNetworkMode("host")
                .withRestartPolicy(RestartPolicy.alwaysRestart())
                .withPrivileged(true)
                .withBinds(new Bind("/hadoopfs/fs1/logs/", new Volume("/var/log/"))));
        LOGGER.info("Ambari server started successfully");
        return true;
    }

}
