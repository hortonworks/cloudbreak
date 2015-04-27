package com.sequenceiq.cloudbreak.core.flow.context;

import java.util.List;

import com.github.dockerjava.api.DockerClient;
import com.sequenceiq.cloudbreak.domain.Stack;
import com.sequenceiq.cloudbreak.service.StackContext;

public class DockerContext extends StackContext {

    private DockerClient dockerClient;
    private List<String> dockerImageNames;

    public DockerContext(Stack stack, DockerClient dockerClient, List<String> dockerImageNames) {
        super(stack);
        this.dockerClient = dockerClient;
        this.dockerImageNames = dockerImageNames;
    }

    public DockerClient getDockerClient() {
        return dockerClient;
    }

    public List<String> getDockerImageNames() {
        return dockerImageNames;
    }
}
