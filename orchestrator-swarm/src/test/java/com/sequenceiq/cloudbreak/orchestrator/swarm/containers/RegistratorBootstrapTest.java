package com.sequenceiq.cloudbreak.orchestrator.swarm.containers;

import com.sequenceiq.cloudbreak.orchestrator.containers.ContainerBootstrap;

public class RegistratorBootstrapTest extends AbstractContainerBootstrapTest {
    @Override
    public ContainerBootstrap getTestInstance() {
        return new RegistratorBootstrap(getMockedDockerClient(), DUMMY_IMAGE, DUMMY_NODE, "privateIp", getMockedDockerClientUtil());
    }
}
