package com.sequenceiq.freeipa.api.v1.freeipa.stack.model.common.instance;

public enum InstanceLifeCycle {
    NORMAL,
    SPOT;

    public static InstanceLifeCycle getDefault() {
        return NORMAL;
    }
}
