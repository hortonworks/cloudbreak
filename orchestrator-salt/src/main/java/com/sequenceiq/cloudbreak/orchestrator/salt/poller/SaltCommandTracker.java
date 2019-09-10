package com.sequenceiq.cloudbreak.orchestrator.salt.poller;

import com.sequenceiq.cloudbreak.orchestrator.OrchestratorBootstrap;
import com.sequenceiq.cloudbreak.orchestrator.exception.CloudbreakOrchestratorFailedException;
import com.sequenceiq.cloudbreak.orchestrator.salt.client.SaltConnector;

public class SaltCommandTracker implements OrchestratorBootstrap {

    private final SaltConnector saltConnector;

    private final SaltJobRunner saltJobRunner;

    public SaltCommandTracker(SaltConnector saltConnector, SaltJobRunner saltJobRunner) {
        this.saltConnector = saltConnector;
        this.saltJobRunner = saltJobRunner;
    }

    @Override
    public Boolean call() throws Exception {
        saltJobRunner.submit(saltConnector);
        if (!saltJobRunner.getTargetHostnames().isEmpty()) {
            throw new CloudbreakOrchestratorFailedException("There are missing nodes from job result: " + saltJobRunner.getTargetHostnames());
        }
        return true;
    }

    @Override
    public String toString() {
        return "SaltCommandTracker{"
                + "saltJobRunner=" + saltJobRunner
                + '}';
    }
}
