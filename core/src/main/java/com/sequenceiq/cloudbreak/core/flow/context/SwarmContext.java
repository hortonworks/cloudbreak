package com.sequenceiq.cloudbreak.core.flow.context;

import java.util.Set;

import com.github.dockerjava.api.DockerClient;
import com.sequenceiq.cloudbreak.domain.Stack;
import com.sequenceiq.cloudbreak.service.StackContext;

public class SwarmContext extends StackContext {

    private DockerClient dockerClient;
    private Set<String> swarmAgents;

    public SwarmContext(Stack stack, DockerClient dockerClient, Set<String> swarmAgents) {
        super(stack);
        this.dockerClient = dockerClient;
        this.swarmAgents = swarmAgents;
    }

    public DockerClient getDockerClient() {
        return dockerClient;
    }

    public Set<String> getSwarmAgents() {
        return swarmAgents;
    }
}
