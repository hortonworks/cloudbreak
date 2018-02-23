package com.sequenceiq.cloudbreak.api.model;

public enum ResourceQueryStatus {
    SUCCESS, FAILED;

    public static ResourceQueryStatus fromQueryStatus(String queryStatus) {
        return SUCCESS.name().equals(queryStatus) ? SUCCESS : FAILED;
    }
}
