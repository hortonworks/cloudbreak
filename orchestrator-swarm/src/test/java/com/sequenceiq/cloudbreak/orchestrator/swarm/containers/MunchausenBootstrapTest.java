package com.sequenceiq.cloudbreak.orchestrator.swarm.containers;

import com.sequenceiq.cloudbreak.orchestrator.containers.ContainerBootstrap;

public class MunchausenBootstrapTest extends AbstractContainerBootstrapTest {

    @Override
    public ContainerBootstrap getTestInstance() {
        return new MunchausenBootstrap(getMockedDockerClient(), DUMMY_IMAGE, CMD, getMockedDockerClientUtil());
    }
}