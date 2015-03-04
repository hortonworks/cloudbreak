package com.sequenceiq.periscope.monitor.handler;

import com.sequenceiq.periscope.domain.Cluster;

public abstract class AbstractResult {

    private final boolean alarmHit;
    private final Cluster cluster;

    public AbstractResult(boolean alarmHit, Cluster cluster) {
        this.alarmHit = alarmHit;
        this.cluster = cluster;
    }

    public boolean isAlarmHit() {
        return alarmHit;
    }

    public Cluster getCluster() {
        return cluster;
    }
}
