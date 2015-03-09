package com.sequenceiq.periscope.monitor.handler;

import com.sequenceiq.periscope.domain.Cluster;

public abstract class BaseResult {

    private final boolean alarmHit;
    private final Cluster cluster;

    public BaseResult(boolean alarmHit, Cluster cluster) {
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
