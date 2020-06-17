package com.sequenceiq.cloudbreak.orchestrator.salt.domain;

import java.util.List;

public class FingerprintRequest {

    private List<Minion> minions;

    public FingerprintRequest() {
    }

    public FingerprintRequest(List<Minion> minions) {
        this.minions = minions;
    }

    public List<Minion> getMinions() {
        return minions;
    }

    public void setMinions(List<Minion> minions) {
        this.minions = minions;
    }
}
