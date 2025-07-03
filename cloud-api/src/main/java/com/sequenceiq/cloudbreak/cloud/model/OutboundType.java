package com.sequenceiq.cloudbreak.cloud.model;

public enum OutboundType {

    LOAD_BALANCER,
    DEFAULT,
    NOT_DEFINED,
    PUBLIC_IP,
    USER_ASSIGNED_NATGATEWAY,
    USER_DEFINED_ROUTING;

    public boolean isDefault() {
        return this == DEFAULT;
    }

    public boolean shouldSync() {
        return this == NOT_DEFINED || isDefault();
    }
}
