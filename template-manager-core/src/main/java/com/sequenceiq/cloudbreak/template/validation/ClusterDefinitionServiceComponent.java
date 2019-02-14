package com.sequenceiq.cloudbreak.template.validation;

import java.util.Collection;
import java.util.List;

import com.google.common.collect.Lists;
import com.sequenceiq.cloudbreak.domain.stack.cluster.host.HostGroup;

public class ClusterDefinitionServiceComponent {

    private final String name;

    private int nodeCount;

    private final List<String> hostgroups;

    public ClusterDefinitionServiceComponent(String name, String hostgroup, int nodeCount) {
        this.name = name;
        this.nodeCount = nodeCount;
        hostgroups = Lists.newArrayList(hostgroup);
    }

    public void update(HostGroup hostGroup) {
        nodeCount += hostGroup.getConstraint().getHostCount();
        hostgroups.add(hostGroup.getName());
    }

    public String getName() {
        return name;
    }

    public int getNodeCount() {
        return nodeCount;
    }

    public Collection<String> getHostgroups() {
        return hostgroups;
    }
}
