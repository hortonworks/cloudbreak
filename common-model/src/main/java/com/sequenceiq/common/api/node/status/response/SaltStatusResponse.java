package com.sequenceiq.common.api.node.status.response;

public class SaltStatusResponse {

    private SaltMasterStatus master;

    private SaltMinionsStatus minions;

    public SaltMasterStatus getMaster() {
        return master;
    }

    public void setMaster(SaltMasterStatus master) {
        this.master = master;
    }

    public SaltMinionsStatus getMinions() {
        return minions;
    }

    public void setMinions(SaltMinionsStatus minions) {
        this.minions = minions;
    }

    @Override
    public String toString() {
        return "SaltStatusResponse{" +
                "master=" + master +
                ", minions=" + minions +
                '}';
    }
}
