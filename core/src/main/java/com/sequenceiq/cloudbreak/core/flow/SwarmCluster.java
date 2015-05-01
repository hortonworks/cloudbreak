package com.sequenceiq.cloudbreak.core.flow;

import java.util.Set;

import com.github.dockerjava.api.DockerClient;

public class SwarmCluster extends ContainerOrchestratorCluster {
    private DockerClient dockerClient;

    public SwarmCluster(String apiAddress, Set<Node> nodes, DockerClient dockerClient) {
        super(apiAddress, nodes);
        this.dockerClient = dockerClient;
    }

    public DockerClient getDockerClient() {
        return dockerClient;
    }
}
