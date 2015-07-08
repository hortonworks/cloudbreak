package com.sequenceiq.cloudbreak.orchestrator.swarm.containers;

import com.sequenceiq.cloudbreak.orchestrator.containers.ContainerBootstrap;

public class ConsulWatchBootstrapTest extends AbstractContainerBootstrapTest {

    @Override
    public ContainerBootstrap getTestInstance() {
        return new ConsulWatchBootstrap(getMockedDockerClient(), DUMMY_IMAGE, DUMMY_GENERATED_ID, getMockedDockerClientUtil());
    }
}
