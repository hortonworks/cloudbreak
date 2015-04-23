package com.sequenceiq.it.cloudbreak;

public class InstanceGroup {
    private String templateId;
    private String name;
    private int nodeCount;
    private String type;

    public InstanceGroup(String templateId, String name, int nodeCount, String type) {
        this.templateId = templateId;
        this.name = name;
        this.nodeCount = nodeCount;
        this.type = type;
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

    public String getType() {
        return type;
    }
}
