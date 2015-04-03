package com.sequenceiq.cloudbreak.core.flow.containers;

import static com.sequenceiq.cloudbreak.service.cluster.flow.DockerContainer.AMBARI_SERVER;

import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.api.command.CreateContainerResponse;
import com.github.dockerjava.api.model.ExposedPort;
import com.github.dockerjava.api.model.HostConfig;
import com.github.dockerjava.api.model.PortBinding;
import com.github.dockerjava.api.model.Ports;
import com.github.dockerjava.api.model.RestartPolicy;

public class AmbariServerBootstrap {

    private static final int PORT = 8080;
    private final DockerClient docker;
    private final String privateIp;
    private final String databaseIp;
    private final String ambariDockerTag;

    public AmbariServerBootstrap(DockerClient docker, String privateIp, String databaseIp, String ambariDockerTag) {
        this.docker = docker;
        this.privateIp = privateIp;
        this.databaseIp = databaseIp;
        this.ambariDockerTag = ambariDockerTag;
    }

    public Boolean call() {
        HostConfig hostConfig = new HostConfig();
        hostConfig.setNetworkMode("host");
        hostConfig.setPrivileged(true);
        hostConfig.setRestartPolicy(RestartPolicy.alwaysRestart());
        Ports ports = new Ports();
        ports.add(new PortBinding(new Ports.Binding(PORT), new ExposedPort(PORT)));
        hostConfig.setPortBindings(ports);

        CreateContainerResponse response = docker.createContainerCmd(ambariDockerTag)
                .withHostConfig(hostConfig)
                .withExposedPorts(new ExposedPort(PORT))
                .withEnv(String.format("constraint:type==gateway"),
                        String.format("BRIDGE_IP=%s", privateIp),
                        String.format("POSTGRES_DB=%s", databaseIp),
                        String.format("SERVICE_NAME=%s", "ambari-8080"))
                .withName(AMBARI_SERVER.getName())
                .withCmd("/start-server")
                .exec();
        docker.startContainerCmd(response.getId())
                .withPortBindings(new PortBinding(new Ports.Binding("0.0.0.0", PORT), new ExposedPort(PORT)))
                .withNetworkMode("host")
                .withRestartPolicy(RestartPolicy.alwaysRestart())
                .withPrivileged(true)
                .exec();
        return true;
    }
}
