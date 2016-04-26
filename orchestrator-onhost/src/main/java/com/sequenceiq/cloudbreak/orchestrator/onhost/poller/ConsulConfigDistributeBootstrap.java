package com.sequenceiq.cloudbreak.orchestrator.onhost.poller;

import java.util.Set;

import com.sequenceiq.cloudbreak.orchestrator.OrchestratorBootstrap;
import com.sequenceiq.cloudbreak.orchestrator.onhost.client.OnHostClient;

public class ConsulConfigDistributeBootstrap implements OrchestratorBootstrap {

    private final OnHostClient client;
    private Set<String> missingTargets;

    public ConsulConfigDistributeBootstrap(OnHostClient client) {
        this.client = client;
        this.missingTargets = client.getTargets();
    }

    @Override
    public Boolean call() throws Exception {
        missingTargets = client.distributeConsuleConfig(missingTargets);
        if (missingTargets.isEmpty()) {
            return true;
        }
        return false;
    }
}
