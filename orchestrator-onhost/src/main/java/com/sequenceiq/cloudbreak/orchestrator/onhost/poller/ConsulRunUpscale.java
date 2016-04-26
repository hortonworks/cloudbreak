package com.sequenceiq.cloudbreak.orchestrator.onhost.poller;

import com.sequenceiq.cloudbreak.orchestrator.OrchestratorBootstrap;
import com.sequenceiq.cloudbreak.orchestrator.onhost.client.OnHostClient;

import java.util.Set;

public class ConsulRunUpscale implements OrchestratorBootstrap {

    private final OnHostClient client;
    private Set<String> missingTargets;

    public ConsulRunUpscale(OnHostClient client, Set<String> missingTargets) {
        this.client = client;
        this.missingTargets = missingTargets;
    }

    @Override
    public Boolean call() throws Exception {
//        missingTargets = client.startConsulServiceOnTargetMachines(missingTargets);
//        if (!missingTargets.isEmpty()) {
//            throw new CloudbreakOrchestratorFailedException("There are missing nodes to start the consul: " + missingTargets);
//        }
        return true;
    }
}
