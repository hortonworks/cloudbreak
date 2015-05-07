package com.sequenceiq.cloudbreak.core.flow;

import java.util.Set;

import com.github.dockerjava.api.DockerClient;
import com.sequenceiq.cloudbreak.orcestrator.ContainerOrchestratorCluster;
import com.sequenceiq.cloudbreak.orcestrator.Node;

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
