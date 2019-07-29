package com.sequenceiq.cloudbreak.api.endpoint.v4.util.base;

public enum RightV4 {
    DISTROX_READ("datahub", "read"),
    DISTROX_WRITE("datahub", "write"),
    SDX_READ("datalake", "read"),
    SDX_WRITE("datalake", "write"),
    ENVIRONMENT_READ("environment", "read"),
    ENVIRONMENT_WRITE("environment", "write");

    private String resource;

    private String action;

    RightV4(String resource, String action) {
        this.resource = resource;
        this.action = action;
    }

    public String getResource() {
        return resource;
    }

    public String getAction() {
        return action;
    }
}
