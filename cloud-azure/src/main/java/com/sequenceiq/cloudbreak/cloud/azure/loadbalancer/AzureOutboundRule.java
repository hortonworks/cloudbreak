package com.sequenceiq.cloudbreak.cloud.azure.loadbalancer;

import static com.google.common.base.Preconditions.checkNotNull;

import com.sequenceiq.cloudbreak.cloud.model.Group;

public final class AzureOutboundRule {
    private final String name;

    private final String groupName;

    public AzureOutboundRule(Group group) {
        this.groupName = checkNotNull(group, "Group must be provided.").getName();
        this.name = defaultNameFromGroup(group.getName());
    }

    private String defaultNameFromGroup(String groupName) {
        return "group-" + groupName + "-outbound-rule";
    }

    public String getName() {
        return name;
    }

    public String getGroupName() {
        return groupName;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("class AzureOutboundRule {\n");
        sb.append("    name: ").append(name).append("\n");
        sb.append("    groupName: ").append(name).append("\n");
        return sb.toString();
    }
}
