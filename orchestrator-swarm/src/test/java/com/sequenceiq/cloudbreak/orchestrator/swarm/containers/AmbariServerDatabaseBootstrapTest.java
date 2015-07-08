package com.sequenceiq.cloudbreak.orchestrator.swarm.containers;

import com.sequenceiq.cloudbreak.orchestrator.containers.ContainerBootstrap;

public class AmbariServerDatabaseBootstrapTest extends AbstractContainerBootstrapTest {

    @Override
    public ContainerBootstrap getTestInstance() {
        return new AmbariServerDatabaseBootstrap(getMockedDockerClient(), DUMMY_IMAGE, DUMMY_NODE, DUMMY_VOLUMES,
                getMockedDockerClientUtil());
    }
}
