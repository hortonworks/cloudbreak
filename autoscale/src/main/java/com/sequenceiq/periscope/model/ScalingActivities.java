package com.sequenceiq.periscope.model;

import java.util.Set;

import com.sequenceiq.periscope.monitor.Monitored;

public class ScalingActivities implements Monitored {

    private long id;

    private Set<Long> activityIds;

    private long lastEvaluated;

    public ScalingActivities() {
    }

    public ScalingActivities(long id, Set<Long> activityIds) {
        this.id = id;
        this.activityIds = activityIds;
    }

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public Set<Long> getActivityIds() {
        return activityIds;
    }

    public void setActivityIds(Set<Long> activityIds) {
        this.activityIds = activityIds;
    }

    public long getLastEvaluated() {
        return lastEvaluated;
    }

    @Override
    public void setLastEvaluated(long lastEvaluated) {
        this.lastEvaluated = lastEvaluated;
    }

    @Override
    public String toString() {
        return "ScalingActivities{" +
                "activities=" + activityIds +
                ", lastEvaluated=" + lastEvaluated +
                '}';
    }
}
