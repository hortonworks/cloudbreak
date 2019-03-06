package com.sequenceiq.cloudbreak.core.flow2.event;

public class ClusterDownscaleDetails {
    private final boolean forced;

    public ClusterDownscaleDetails() {
        forced = false;
    }

    public ClusterDownscaleDetails(boolean forced) {
        this.forced = forced;
    }

    public boolean isForced() {
        return forced;
    }

}
