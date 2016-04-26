package com.sequenceiq.cloudbreak.orchestrator.swarm.containers;

import com.sequenceiq.cloudbreak.orchestrator.OrchestratorBootstrap;

public class MunchausenBootstrapTest extends AbstractContainerBootstrapTest {

    @Override
    public OrchestratorBootstrap getTestInstance() {
        return new MunchausenBootstrap(getMockedDockerClient(), DUMMY_IMAGE, CMD);
    }
}