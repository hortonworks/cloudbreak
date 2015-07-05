package com.sequenceiq.cloudbreak.orchestrator.swarm.containers;

import com.sequenceiq.cloudbreak.orchestrator.containers.ContainerBootstrap;

public class AmbariAgentBoostrapTest extends AbstractContainerBootstrapTest {

    @Override
    public ContainerBootstrap getTestInstance() {
        return new AmbariAgentBootstrap(getMockedDockerClient(), DUMMY_IMAGE, DUMMY_NODE, DUMMY_VOLUMES,
                DUMMY_GENERATED_ID, DUMMY_CLOUD_PLATFORM, getMockedDockerClientUtil());
    }
}
