package com.sequenceiq.cloudbreak.orchestrator.swarm.containers

import com.sequenceiq.cloudbreak.orchestrator.OrchestratorBootstrap

class MunchausenBootstrapTest : AbstractContainerBootstrapTest() {

    override val testInstance: OrchestratorBootstrap
        get() = MunchausenBootstrap(mockedDockerClient, AbstractContainerBootstrapTest.DUMMY_IMAGE, AbstractContainerBootstrapTest.CMD)
}