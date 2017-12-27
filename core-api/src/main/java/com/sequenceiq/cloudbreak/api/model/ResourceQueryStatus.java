package com.sequenceiq.cloudbreak.api.model;

public enum ResourceQueryStatus {
    SUCCESS, FAILED;

    public static ResourceQueryStatus fromQueryStatus(String queryStatus) {
        if (SUCCESS.name().equals(queryStatus)) {
            return SUCCESS;
        } else {
            return FAILED;
        }
    }
}
