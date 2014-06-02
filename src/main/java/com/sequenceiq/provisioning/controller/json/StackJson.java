package com.sequenceiq.provisioning.controller.json;

import javax.validation.constraints.Min;
import javax.validation.constraints.Size;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.sequenceiq.provisioning.domain.CloudPlatform;
import com.sequenceiq.provisioning.domain.StackDescription;
import com.sequenceiq.provisioning.domain.Status;

public class StackJson implements JsonEntity {

    private Long id;
    @Min(value = 2)
    private int nodeCount;
    @Size(max = 20)
    private String name;
    private Long templateId;
    private CloudPlatform cloudPlatform;
    private StackDescription description;
    private Long credentialId;
    private Status status;
    private String ambariServerIp;

    public StackJson() {
    }

    @JsonProperty("id")
    public Long getId() {
        return id;
    }

    @JsonIgnore
    public void setId(Long id) {
        this.id = id;
    }

    public int getNodeCount() {
        return nodeCount;
    }

    public void setNodeCount(int nodeCount) {
        this.nodeCount = nodeCount;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Long getTemplateId() {
        return templateId;
    }

    public void setTemplateId(Long templateId) {
        this.templateId = templateId;
    }

    @JsonProperty("cloudPlatform")
    public CloudPlatform getCloudPlatform() {
        return cloudPlatform;
    }

    public Long getCredentialId() {
        return credentialId;
    }

    public void setCredentialId(Long credentialId) {
        this.credentialId = credentialId;
    }

    @JsonIgnore
    public void setCloudPlatform(CloudPlatform cloudPlatform) {
        this.cloudPlatform = cloudPlatform;
    }

    @JsonProperty("description")
    public StackDescription getDescription() {
        return description;
    }

    @JsonIgnore
    public void setDescription(StackDescription description) {
        this.description = description;
    }

    @JsonProperty("status")
    public Status getStatus() {
        return status;
    }

    @JsonIgnore
    public void setStatus(Status status) {
        this.status = status;
    }

    @JsonProperty("ambariServerIp")
    public String getAmbariServerIp() {
        return ambariServerIp;
    }

    @JsonIgnore
    public void setAmbariServerIp(String ambariServerIp) {
        this.ambariServerIp = ambariServerIp;
    }
}
