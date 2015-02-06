package com.sequenceiq.it.cloudbreak;

public class InstanceGroup {
    private String templateId;
    private String name;
    private int nodeCount;

    public InstanceGroup(String templateId, String name, int nodeCount) {
        this.templateId = templateId;
        this.name = name;
        this.nodeCount = nodeCount;
    }

    public String getTemplateId() {
        return templateId;
    }

    public String getName() {
        return name;
    }

    public int getNodeCount() {
        return nodeCount;
    }
}
