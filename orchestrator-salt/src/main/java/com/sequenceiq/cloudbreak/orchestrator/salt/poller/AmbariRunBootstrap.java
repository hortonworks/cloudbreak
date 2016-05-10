package com.sequenceiq.cloudbreak.orchestrator.salt.poller;

import com.sequenceiq.cloudbreak.orchestrator.OrchestratorBootstrap;
import com.sequenceiq.cloudbreak.orchestrator.salt.client.SaltConnector;

public class AmbariRunBootstrap implements OrchestratorBootstrap {

    private final SaltConnector saltConnector;

    public AmbariRunBootstrap(SaltConnector saltConnector) {
        this.saltConnector = saltConnector;
    }

    @Override
    public Boolean call() throws Exception {
        return true;
    }
}
