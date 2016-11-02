package com.sequenceiq.cloudbreak.shell.model;

public class InstanceGroupEntry {

    private Long templateId;

    private Integer nodeCount;

    private String type;

    private Long securityGroupId;

    public InstanceGroupEntry(Long templateId, Long securityGroupId, Integer nodeCount, String type) {
        this.templateId = templateId;
        this.nodeCount = nodeCount;
        this.type = type;
        this.securityGroupId = securityGroupId;
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

    public Long getSecurityGroupId() {
        return securityGroupId;
    }
}
