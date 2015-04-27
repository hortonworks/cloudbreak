package com.sequenceiq.cloudbreak.domain;

public enum InstanceGroupType {
    GATEWAY(1), CORE(0);

    private Integer fixedNodeCount;

    private InstanceGroupType(Integer fixedNodeCount) {
        this.fixedNodeCount = fixedNodeCount;
    }

    public Integer getFixedNodeCount() {
        return fixedNodeCount;
    }

    public static boolean isGateway(InstanceGroupType type) {
        return GATEWAY.equals(type);
    }

    public static boolean isCoreGroup(InstanceGroupType type) {
        return CORE.equals(type);
    }
}