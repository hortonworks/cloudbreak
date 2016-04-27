package com.sequenceiq.cloudbreak.orchestrator.onhost.poller;

import java.util.Set;

import com.sequenceiq.cloudbreak.orchestrator.OrchestratorBootstrap;
import com.sequenceiq.cloudbreak.orchestrator.exception.CloudbreakOrchestratorFailedException;
import com.sequenceiq.cloudbreak.orchestrator.onhost.client.OnHostClient;

public class ConsulRunUpscale implements OrchestratorBootstrap {

    private final OnHostClient client;
    private Set<String> missingTargets;

    public ConsulRunUpscale(OnHostClient client, Set<String> missingTargets) {
        this.client = client;
        this.missingTargets = missingTargets;
    }

    @Override
    public Boolean call() throws Exception {
        missingTargets = client.startConsulServiceOnTargetMachines(missingTargets);
        if (!missingTargets.isEmpty()) {
            throw new CloudbreakOrchestratorFailedException("There are missing nodes to start the consul: " + missingTargets);
        }
        return true;
    }
}
