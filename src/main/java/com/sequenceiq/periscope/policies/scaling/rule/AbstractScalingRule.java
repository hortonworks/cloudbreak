package com.sequenceiq.periscope.policies.scaling.rule;

import org.apache.hadoop.yarn.server.resourcemanager.webapp.dao.ClusterMetricsInfo;

public abstract class AbstractScalingRule implements ScalingRule {

    private String name;
    private int limit;
    private int scalingAdjustment;

    @Override
    public String getName() {
        return name;
    }

    @Override
    public int getLimit() {
        return limit;
    }

    @Override
    public int getScalingAdjustment() {
        return scalingAdjustment;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setLimit(String limit) {
        this.limit = toInt(limit);
    }

    public void setScalingAdjustment(String scalingAdjustment) {
        this.scalingAdjustment = toInt(scalingAdjustment);
    }

    protected int getCurrentNodeCount(ClusterMetricsInfo metricsInfo) {
        return metricsInfo.getTotalNodes();
    }

    protected int scaleTo(int currentNodeCount, int desiredNodeCount) {
        return desiredNodeCount == currentNodeCount ? 0 : desiredNodeCount;
    }

    private int toInt(String value) {
        return value == null ? 0 : Integer.valueOf(value);
    }
}
