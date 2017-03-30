package com.sequenceiq.cloudbreak.api.model;

public enum InstanceGroupType {
    GATEWAY, CORE;

    public static boolean isGateway(InstanceGroupType type) {
        return GATEWAY.equals(type);
    }
}