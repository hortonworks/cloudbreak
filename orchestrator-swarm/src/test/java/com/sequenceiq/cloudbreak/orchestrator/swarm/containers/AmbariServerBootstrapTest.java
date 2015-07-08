package com.sequenceiq.cloudbreak.orchestrator.swarm.containers;

import com.sequenceiq.cloudbreak.orchestrator.containers.ContainerBootstrap;

public class AmbariServerBootstrapTest extends AbstractContainerBootstrapTest {

    @Override
    public ContainerBootstrap getTestInstance() {
        return new AmbariServerBootstrap(getMockedDockerClient(), DUMMY_IMAGE, DUMMY_NODE, DUMMY_VOLUMES,
                DUMMY_CLOUD_PLATFORM, getMockedDockerClientUtil());
    }
}
