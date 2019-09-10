package com.sequenceiq.cloudbreak.core.flow2.event;

public class ClusterDownscaleDetails {
    private final boolean forced;

    private final boolean repair;

    public ClusterDownscaleDetails() {
        forced = false;
        repair = false;
    }

    public ClusterDownscaleDetails(boolean forced, boolean repair) {
        this.forced = forced;
        this.repair = repair;
    }

    public boolean isForced() {
        return forced;
    }

    public boolean isRepair() {
        return repair;
    }
}
