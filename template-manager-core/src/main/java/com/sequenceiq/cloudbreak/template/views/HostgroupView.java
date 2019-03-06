package com.sequenceiq.cloudbreak.template.views;

import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.base.InstanceGroupType;

public class HostgroupView {

    private final String name;

    private final Integer volumeCount;

    private final boolean instanceGroupConfigured;

    private final InstanceGroupType instanceGroupType;

    private final Integer nodeCount;

    public HostgroupView(String name, int volumeCount, InstanceGroupType instanceGroupType, Integer nodeCount) {
        this.name = name;
        this.volumeCount = volumeCount;
        instanceGroupConfigured = true;
        this.instanceGroupType = instanceGroupType;
        this.nodeCount = nodeCount;
    }

    public HostgroupView(String name) {
        this.name = name;
        volumeCount = null;
        instanceGroupConfigured = false;
        instanceGroupType = InstanceGroupType.CORE;
        nodeCount = 0;
    }

    public String getName() {
        return name;
    }

    public Integer getVolumeCount() {
        return volumeCount;
    }

    public boolean isInstanceGroupConfigured() {
        return instanceGroupConfigured;
    }

    public InstanceGroupType getInstanceGroupType() {
        return instanceGroupType;
    }

    public Integer getNodeCount() {
        return nodeCount;
    }
}
