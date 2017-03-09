package com.sequenceiq.cloudbreak.shell.model;

import java.util.Map;

public class InstanceGroupEntry {

    private Long templateId;

    private Integer nodeCount;

    private String type;

    private Long securityGroupId;

    private Map<String, Object> attributes;

    public InstanceGroupEntry(Long templateId, Long securityGroupId, Integer nodeCount, String type, Map<String, Object>  attributes) {
        this.templateId = templateId;
        this.nodeCount = nodeCount;
        this.type = type;
        this.securityGroupId = securityGroupId;
        this.attributes = attributes;
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

    public void setType(String type) {
        this.type = type;
    }

    public Map<String, Object> getAttributes() {
        return attributes;
    }
}
