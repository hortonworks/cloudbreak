package com.sequenceiq.cloudbreak.orchestrator.onhost.poller;

import java.util.Set;

import com.sequenceiq.cloudbreak.orchestrator.OrchestratorBootstrap;
import com.sequenceiq.cloudbreak.orchestrator.exception.CloudbreakOrchestratorFailedException;
import com.sequenceiq.cloudbreak.orchestrator.onhost.client.OnHostClient;

public class ConsulRunBootstrap implements OrchestratorBootstrap {

    private final OnHostClient client;
    private Set<String> missingTargets;

    public ConsulRunBootstrap(OnHostClient client) {
        this.client = client;
        this.missingTargets = client.getTargets();
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
