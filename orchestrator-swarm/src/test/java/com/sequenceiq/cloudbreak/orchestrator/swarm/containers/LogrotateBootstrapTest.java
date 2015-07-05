package com.sequenceiq.cloudbreak.orchestrator.swarm.containers;

import com.sequenceiq.cloudbreak.orchestrator.containers.ContainerBootstrap;

public class LogrotateBootstrapTest extends AbstractContainerBootstrapTest {

    @Override
    public ContainerBootstrap getTestInstance() {
        return new LogrotateBootsrap(getMockedDockerClient(), DUMMY_IMAGE, DUMMY_GENERATED_ID, getMockedDockerClientUtil());
    }
}
