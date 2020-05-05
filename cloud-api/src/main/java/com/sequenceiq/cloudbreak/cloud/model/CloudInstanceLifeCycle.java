package com.sequenceiq.cloudbreak.cloud.model;

public enum CloudInstanceLifeCycle {
    NORMAL,
    SPOT;

    public static CloudInstanceLifeCycle getDefault() {
        return NORMAL;
    }
}
