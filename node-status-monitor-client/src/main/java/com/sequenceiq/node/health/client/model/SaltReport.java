package com.sequenceiq.node.health.client.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public class SaltReport {

    private SaltMasterReport master;

    private SaltMinionsReport minions;

    public SaltMasterReport getMaster() {
        return master;
    }

    public void setMaster(SaltMasterReport master) {
        this.master = master;
    }

    public SaltMinionsReport getMinions() {
        return minions;
    }

    public void setMinions(SaltMinionsReport minions) {
        this.minions = minions;
    }

    @Override
    public String toString() {
        return "SaltReport{" +
                "master=" + master +
                ", minions=" + minions +
                '}';
    }
}
