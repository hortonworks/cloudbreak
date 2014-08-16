package com.sequenceiq.periscope.policies.scaling.rule;

public abstract class AbstractScalingRule implements ScalingRule {

    private String name;
    private int limit;

    @Override
    public String getName() {
        return name;
    }

    @Override
    public int getLimit() {
        return limit;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setLimit(int limit) {
        this.limit = limit;
    }
}
