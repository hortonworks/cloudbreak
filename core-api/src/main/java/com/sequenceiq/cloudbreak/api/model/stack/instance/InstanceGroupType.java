package com.sequenceiq.cloudbreak.api.model.stack.instance;

public enum InstanceGroupType {
    GATEWAY, CORE;

    public static boolean isGateway(InstanceGroupType type) {
        return GATEWAY.equals(type);
    }
}