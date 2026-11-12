package com.sequenceiq.cloudbreak.orchestrator.salt.states;

import java.util.Set;

import com.sequenceiq.cloudbreak.orchestrator.exception.CloudbreakOrchestratorFailedException;

public record SaltMastersStopResult(Set<String> mastersNotStopped, RuntimeException stopRequestFailure) {

    public static SaltMastersStopResult noStopRequested() {
        return new SaltMastersStopResult(Set.of(), null);
    }

    public static SaltMastersStopResult withMastersNotStopped(Set<String> mastersNotStopped) {
        return new SaltMastersStopResult(mastersNotStopped, null);
    }

    public static SaltMastersStopResult stopRequestFailed(Set<String> mastersInUnknownState, RuntimeException stopRequestFailure) {
        return new SaltMastersStopResult(mastersInUnknownState, stopRequestFailure);
    }

    public void failIfAnyMasterWasNotStopped() throws CloudbreakOrchestratorFailedException {
        if (!mastersNotStopped.isEmpty()) {
            throw new CloudbreakOrchestratorFailedException("Failed to restart salt master on gateway(s) " + mastersNotStopped
                    + " to load its new configuration, please check the salt-bootstrap service status on them and retry the operation.",
                    stopRequestFailure);
        }
    }
}
