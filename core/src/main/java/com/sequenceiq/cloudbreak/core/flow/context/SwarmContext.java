package com.sequenceiq.cloudbreak.core.flow.context;

import com.github.dockerjava.api.DockerClient;
import com.sequenceiq.cloudbreak.domain.Stack;
import com.sequenceiq.cloudbreak.service.StackContext;

public class SwarmContext extends StackContext {

    private DockerClient dockerClient;
    private int nodeCount;

    public SwarmContext(Stack stack, DockerClient dockerClient, int nodeCount) {
        super(stack);
        this.dockerClient = dockerClient;
        this.nodeCount = nodeCount;
    }

    public DockerClient getDockerClient() {
        return dockerClient;
    }

    public int getNodeCount() {
        return nodeCount;
    }
}
