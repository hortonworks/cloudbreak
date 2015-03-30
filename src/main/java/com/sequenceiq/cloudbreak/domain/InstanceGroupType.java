package com.sequenceiq.cloudbreak.domain;

public enum InstanceGroupType {
    GATEWAY(1), HOSTGROUP(0);

    private Integer fixedNodeCount;

    private InstanceGroupType(Integer fixedNodeCount) {
        this.fixedNodeCount = fixedNodeCount;
    }

    public Integer getFixedNodeCount() {
        return fixedNodeCount;
    }

    public static boolean isGateWay(InstanceGroupType type) {
        return GATEWAY.equals(type);
    }

    public static boolean isHostGroup(InstanceGroupType type) {
        return HOSTGROUP.equals(type);
    }
}