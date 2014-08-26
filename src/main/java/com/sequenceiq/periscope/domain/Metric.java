package com.sequenceiq.periscope.domain;

public enum Metric {

    PENDING_CONTAINERS("pending containers"),
    PENDING_APPLICATIONS("pending applications"),
    LOST_NODES("lost nodes"),
    UNHEALTHY_NODES("unhealthy nodes"),
    GLOBAL_RESOURCES("global resources");

    private final String name;

    private Metric(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }
}
