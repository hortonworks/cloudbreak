package com.sequenceiq.it.cloudbreak;

class TemplateAddition {
    private String groupName;
    private int nodeCount;

    public TemplateAddition(String groupName, int nodeCount) {
        this.groupName = groupName;
        this.nodeCount = nodeCount;
    }

    public String getGroupName() {
        return groupName;
    }

    public int getNodeCount() {
        return nodeCount;
    }
}
