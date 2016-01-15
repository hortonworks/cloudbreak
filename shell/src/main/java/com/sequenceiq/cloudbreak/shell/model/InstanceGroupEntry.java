package com.sequenceiq.cloudbreak.shell.model;

public class InstanceGroupEntry {

    private Long templateId;
    private Integer nodeCount;
    private String type;

    public InstanceGroupEntry(Long templateId, Integer nodeCount, String type) {
        this.templateId = templateId;
        this.nodeCount = nodeCount;
        this.type = type;
    }

    public Integer getNodeCount() {
        return nodeCount;
    }

    public Long getTemplateId() {
        return templateId;
    }

    public String getType() {
        return type;
    }
}
