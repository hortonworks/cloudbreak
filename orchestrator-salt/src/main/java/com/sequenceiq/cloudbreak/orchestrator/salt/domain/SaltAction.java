package com.sequenceiq.cloudbreak.orchestrator.salt.domain;

import com.sequenceiq.cloudbreak.orchestrator.salt.client.SaltActionType;

import java.util.ArrayList;
import java.util.List;

public class SaltAction {

    private SaltActionType action;

    private String server;

    private List<Minion> minions;

    public SaltAction(SaltActionType action) {
        this.action = action;
    }

    public SaltActionType getAction() {
        return action;
    }

    public String getServer() {
        return server;
    }

    public void setServer(String server) {
        this.server = server;
    }

    public List<Minion> getMinions() {
        return minions;
    }

    public void setMinions(List<Minion> minions) {
        this.minions = minions;
    }

    public void addMinion(Minion minion) {
        if (minions == null) {
            minions = new ArrayList<>();
        }
        minions.add(minion);
    }
}
