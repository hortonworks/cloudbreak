package com.sequenceiq.cloudbreak.orchestrator.salt.poller;

import com.sequenceiq.cloudbreak.orchestrator.OrchestratorBootstrap;
import com.sequenceiq.cloudbreak.orchestrator.salt.client.SaltConnector;
import com.sequenceiq.cloudbreak.orchestrator.salt.client.target.Glob;
import com.sequenceiq.cloudbreak.orchestrator.salt.states.SaltStates;

public class ConsulRunBootstrap implements OrchestratorBootstrap {

    private final SaltConnector saltConnector;

    public ConsulRunBootstrap(SaltConnector saltConnector) {
        this.saltConnector = saltConnector;
    }

    @Override
    public Boolean call() throws Exception {
        SaltStates.consul(saltConnector, Glob.ALL);
        return true;
    }
}
