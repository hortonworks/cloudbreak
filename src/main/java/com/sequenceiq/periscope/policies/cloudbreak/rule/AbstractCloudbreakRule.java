package com.sequenceiq.periscope.policies.cloudbreak.rule;

public abstract class AbstractCloudbreakRule implements CloudbreakRule {

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
