package com.sequenceiq.periscope.model;

public enum HostResolution {
    PUBLIC("public"),
    PRIVATE("private");

    private String name;

    private HostResolution(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }

}
