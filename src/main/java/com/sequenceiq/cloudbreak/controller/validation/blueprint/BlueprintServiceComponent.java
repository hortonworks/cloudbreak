package com.sequenceiq.cloudbreak.controller.validation.blueprint;

import java.util.List;

import com.google.common.collect.Lists;
import com.sequenceiq.cloudbreak.domain.InstanceGroup;

class BlueprintServiceComponent {
    private String name;
    private int nodeCount;
    private List<String> hostgroups;

    public BlueprintServiceComponent(String name, String hostgroup, int nodeCount) {
        this.name = name;
        this.nodeCount = nodeCount;
        this.hostgroups = Lists.newArrayList(hostgroup);
    }

    public void update(InstanceGroup instanceGroup) {
        nodeCount += instanceGroup.getNodeCount();
        hostgroups.add(instanceGroup.getGroupName());
    }

    public String getName() {
        return name;
    }

    public int getNodeCount() {
        return nodeCount;
    }

    public List<String> getHostgroups() {
        return hostgroups;
    }
}
