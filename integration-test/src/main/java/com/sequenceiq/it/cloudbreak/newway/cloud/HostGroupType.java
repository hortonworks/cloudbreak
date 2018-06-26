package com.sequenceiq.it.cloudbreak.newway.cloud;

public enum HostGroupType {

    MASTER("master"),
    WORKER("worker"),
    COMPUTE("compute");

    private final String name;

    HostGroupType(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }
}
