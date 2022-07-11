package com.sequenceiq.cloudbreak.core.flow2.event;

public class ClusterDownscaleDetails {

    private final boolean forced;

    private final boolean repair;

    private final boolean purgeZombies;

    public ClusterDownscaleDetails() {
        forced = false;
        repair = false;
        purgeZombies = false;
    }

    public ClusterDownscaleDetails(boolean forced, boolean repair, boolean purgeZombies) {
        this.forced = forced;
        this.repair = repair;
        this.purgeZombies = purgeZombies;
    }

    public boolean isForced() {
        return forced;
    }

    public boolean isRepair() {
        return repair;
    }

    public boolean isPurgeZombies() {
        return purgeZombies;
    }
}
