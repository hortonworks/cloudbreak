package com.sequenceiq.cloudbreak.it;

public enum RestResource {
    CREDENTIAL("/user/credentials"),
    BLUEPRINT("/user/blueprints"),
    TEMPLATE("/user/templates"),
    STACK("/user/stacks"),
    CLUSTER("/stacks/{stackId}/cluster"),
    STACK_ADJUSTMENT("/stacks/{stackId}"),
    HOSTGROUP_ADJUSTMENT("/stacks/{stackId}/cluster");

    private String path;

    RestResource(String path) {
        this.path = path;
    }

    public String path() {
        return this.path;
    }
}
