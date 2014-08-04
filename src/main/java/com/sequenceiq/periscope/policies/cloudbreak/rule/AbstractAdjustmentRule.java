package com.sequenceiq.periscope.policies.cloudbreak.rule;

public abstract class AbstractAdjustmentRule implements ClusterAdjustmentRule {

    private final String name;
    private final int limit;
    private final int order;

    protected AbstractAdjustmentRule(String name, int order, int limit) {
        this.name = name;
        this.order = order;
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

    @Override
    public int getOrder() {
        return order;
    }

    @Override
    public int compareTo(ClusterAdjustmentRule o) {
        return Integer.compare(order, o.getOrder());
    }
}
