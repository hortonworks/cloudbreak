package com.sequenceiq.cloudbreak.controller.json;

import java.util.HashSet;
import java.util.Set;

import javax.validation.constraints.Digits;
import javax.validation.constraints.Min;
import javax.validation.constraints.Pattern;
import javax.validation.constraints.Size;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.sequenceiq.cloudbreak.domain.CloudPlatform;
import com.sequenceiq.cloudbreak.domain.StackDescription;
import com.sequenceiq.cloudbreak.domain.Status;

public class StackJson implements JsonEntity {

    private Long id;
    @Min(value = 2, message = "Count of nodes has to be min 2")
    @Digits(fraction = 0, integer = 10, message = "Node count has to be a number")
    private int nodeCount;
    @Size(max = 20, min = 2,  message = "Name has to be min 2 letter maximum 20 length")
    @Pattern(regexp = "([a-zA-Z][-a-zA-Z0-9]*)",
            message = "Must contain only alphanumeric characters (case sensitive) and hyphens and start with an alpha character.")
    private String name;
    private Long templateId;
    private CloudPlatform cloudPlatform;
    private StackDescription description;
    private Long credentialId;
    private Status status;
    private String ambariServerIp;
    private String hash;
    private ClusterResponse cluster;
    private Set<MetadataJson> metadata = new HashSet<>();

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

    @JsonProperty("hash")
    public String getHash() {
        return hash;
    }

    @JsonIgnore
    public void setHash(String hash) {
        this.hash = hash;
    }

    @JsonProperty("metadata")
    public Set<MetadataJson> getMetadata() {
        return metadata;
    }

    @JsonIgnore
    public void setMetadata(Set<MetadataJson> metadata) {
        this.metadata = metadata;
    }

    @JsonProperty("cluster")
    public ClusterResponse getCluster() {
        return cluster;
    }

    @JsonIgnore
    public void setCluster(ClusterResponse cluster) {
        this.cluster = cluster;
    }
}
