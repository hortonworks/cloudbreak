package com.sequenceiq.cloudbreak.core.flow;

import com.github.dockerjava.api.DockerClient;

public class SwarmContainerOrchestratorClient implements ContainerOrchestratorClient {
    private DockerClient dockerClient;

    public SwarmContainerOrchestratorClient(DockerClient dockerClient) {
        this.dockerClient = dockerClient;
    }

    public DockerClient getDockerClient() {
        return dockerClient;
    }
}
