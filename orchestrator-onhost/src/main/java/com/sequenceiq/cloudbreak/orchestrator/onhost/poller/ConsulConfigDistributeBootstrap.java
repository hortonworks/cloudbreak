package com.sequenceiq.cloudbreak.orchestrator.onhost.poller;

import java.util.Set;

import com.sequenceiq.cloudbreak.orchestrator.OrchestratorBootstrap;
import com.sequenceiq.cloudbreak.orchestrator.exception.CloudbreakOrchestratorFailedException;
import com.sequenceiq.cloudbreak.orchestrator.onhost.client.OnHostClient;

public class ConsulConfigDistributeBootstrap implements OrchestratorBootstrap {

    private final OnHostClient client;
    private Set<String> missingTargets;

    public ConsulConfigDistributeBootstrap(OnHostClient client, Set<String> missingTargets) {
        this.client = client;
        this.missingTargets = missingTargets;
    }

    @Override
    public Boolean call() throws Exception {
        missingTargets = client.distributeConsulConfig(missingTargets);
        if (!missingTargets.isEmpty()) {
            throw new CloudbreakOrchestratorFailedException("There are missing nodes to distribute the consul config to: " + missingTargets);
        }
        return true;
    }
}
