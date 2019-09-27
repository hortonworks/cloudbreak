package com.sequenceiq.cloudbreak.orchestrator.salt.poller;

import java.util.Collection;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sequenceiq.cloudbreak.orchestrator.OrchestratorBootstrap;
import com.sequenceiq.cloudbreak.orchestrator.salt.client.SaltConnector;

public class SaltCommandTracker implements OrchestratorBootstrap {

    private static final Logger LOGGER = LoggerFactory.getLogger(SaltCommandTracker.class);

    private final SaltConnector saltConnector;

    private final SaltJobRunner saltJobRunner;

    public SaltCommandTracker(SaltConnector saltConnector, SaltJobRunner saltJobRunner) {
        this.saltConnector = saltConnector;
        this.saltJobRunner = saltJobRunner;
    }

    @Override
    public Optional<Collection<String>> call() throws Exception {
        saltJobRunner.submit(saltConnector);
        if (!saltJobRunner.getTargetHostnames().isEmpty()) {
            LOGGER.warn("There are missing nodes from job result: " + saltJobRunner.getTargetHostnames());
            return Optional.of(saltJobRunner.getTargetHostnames());
        }
        return Optional.empty();
    }

    @Override
    public String toString() {
        return "SaltCommandTracker{"
                + "saltJobRunner=" + saltJobRunner
                + '}';
    }
}
