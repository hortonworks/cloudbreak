package com.sequenceiq.cloudbreak.core.flow.context;

import java.util.List;

import com.github.dockerjava.api.DockerClient;
import com.sequenceiq.cloudbreak.domain.Stack;
import com.sequenceiq.cloudbreak.service.StackDependentPollerObject;

public class DockerContext extends StackDependentPollerObject {

    private DockerClient dockerClient;
    private List<String> containerNames;

    public DockerContext(Stack stack, DockerClient dockerClient, List<String> containerNames) {
        super(stack);
        this.dockerClient = dockerClient;
        this.containerNames = containerNames;
    }

    public DockerClient getDockerClient() {
        return dockerClient;
    }

    public List<String> getContainerNames() {
        return containerNames;
    }
}
