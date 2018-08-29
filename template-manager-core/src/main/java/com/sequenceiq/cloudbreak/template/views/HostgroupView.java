package com.sequenceiq.cloudbreak.template.views;

import com.sequenceiq.cloudbreak.api.model.stack.instance.InstanceGroupType;

public class HostgroupView {

    private final String name;

    private final Integer volumeCount;

    private final boolean instanceGroupConfigured;

    private final InstanceGroupType instanceGroupType;

    private final Integer nodeCount;

    public HostgroupView(String name, int volumeCount, InstanceGroupType instanceGroupType, Integer nodeCount) {
        this.name = name;
        this.volumeCount = volumeCount;
        this.instanceGroupConfigured = true;
        this.instanceGroupType = instanceGroupType;
        this.nodeCount = nodeCount;
    }

    public HostgroupView(String name) {
        this.name = name;
        this.volumeCount = null;
        this.instanceGroupConfigured = false;
        this.instanceGroupType = InstanceGroupType.CORE;
        this.nodeCount = 0;
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
