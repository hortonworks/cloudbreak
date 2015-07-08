package com.sequenceiq.cloudbreak.orchestrator.swarm.containers;

import com.sequenceiq.cloudbreak.orchestrator.containers.ContainerBootstrap;

public class BaywatchServerBootstrapTest extends AbstractContainerBootstrapTest {

    @Override
    public ContainerBootstrap getTestInstance() {
        return new BaywatchServerBootstrap(getMockedDockerClient(), DUMMY_IMAGE, DUMMY_NODE, getMockedDockerClientUtil());
    }
}
