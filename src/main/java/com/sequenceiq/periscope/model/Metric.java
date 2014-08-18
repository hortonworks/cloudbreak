package com.sequenceiq.periscope.model;

public enum Metric {

    PENDING_CONTAINERS("containersPending"),
    PENDING_APPLICATIONS("appsPending"),
    LOST_NODES("lostNodes"),
    UNHEALTHY_NODES("unhealthyNodes"),
    GLOBAL_RESOURCES("global");

    private final String name;

    private Metric(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }
}
