package com.sequenceiq.periscope.policies.cloudbreak.rule;

public abstract class AbstractAdjustmentRule implements ClusterAdjustmentRule {

    private final String name;
    private final int limit;

    protected AbstractAdjustmentRule(String name, int limit) {
        this.name = name;
        this.limit = limit;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public int getLimit() {
        return limit;
    }

}
