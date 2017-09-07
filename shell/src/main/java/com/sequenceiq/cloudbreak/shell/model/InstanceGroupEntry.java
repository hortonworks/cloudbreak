package com.sequenceiq.cloudbreak.shell.model;

import java.util.Map;

public class InstanceGroupEntry {

    private final Long templateId;

    private final Integer nodeCount;

    private String type;

    private final Long securityGroupId;

    private final Map<String, Object> attributes;

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

    //BEGIN GENERATED CODE
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        InstanceGroupEntry that = (InstanceGroupEntry) o;

        if (templateId != null ? !templateId.equals(that.templateId) : that.templateId != null) return false;
        if (nodeCount != null ? !nodeCount.equals(that.nodeCount) : that.nodeCount != null) return false;
        if (type != null ? !type.equals(that.type) : that.type != null) return false;
        if (securityGroupId != null ? !securityGroupId.equals(that.securityGroupId) : that.securityGroupId != null) return false;
        return attributes != null ? attributes.equals(that.attributes) : that.attributes == null;
    }

    @Override
    public int hashCode() {
        int result = templateId != null ? templateId.hashCode() : 0;
        result = 31 * result + (nodeCount != null ? nodeCount.hashCode() : 0);
        result = 31 * result + (type != null ? type.hashCode() : 0);
        result = 31 * result + (securityGroupId != null ? securityGroupId.hashCode() : 0);
        result = 31 * result + (attributes != null ? attributes.hashCode() : 0);
        return result;
    }
    //END GENERATED CODE
}
