package com.sequenceiq.cloudbreak.controller.json;

import java.util.HashSet;
import java.util.Set;

import javax.validation.constraints.Digits;
import javax.validation.constraints.Max;
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
    @Min(value = 1, message = "Count of nodes has to be min 1")
    @Max(value = 100000, message = "Count of nodes has to be max 100000")
    @Digits(fraction = 0, integer = 10, message = "Node count has to be a number")
    private int nodeCount;
    @Size(max = 40, min = 5, message = "Name has to be min 5 letter maximum 50 length")
    @Pattern(regexp = "([a-z][-a-z0-9]*[a-z0-9])",
            message = "Must contain only alphanumeric characters (case sensitive) and hyphens and start with an alpha character.")
    private String name;
    private Long templateId;
    private String owner;
    private String account;
    private boolean publicInAccount;
    private CloudPlatform cloudPlatform;
    private StackDescription description;
    private Long credentialId;
    private Status status;
    private String ambariServerIp;
    private String hash;
    private ClusterResponse cluster;
    private String statusReason;
    private Set<InstanceMetaDataJson> metadata = new HashSet<>();

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

    @JsonIgnore
    public void setCloudPlatform(CloudPlatform cloudPlatform) {
        this.cloudPlatform = cloudPlatform;
    }

    public Long getCredentialId() {
        return credentialId;
    }

    public void setCredentialId(Long credentialId) {
        this.credentialId = credentialId;
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

    @JsonProperty("statusReason")
    public String getStatusReason() {
        return statusReason;
    }

    @JsonIgnore
    public void setStatusReason(String statusReason) {
        this.statusReason = statusReason;
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
    public Set<InstanceMetaDataJson> getMetadata() {
        return metadata;
    }

    @JsonIgnore
    public void setMetadata(Set<InstanceMetaDataJson> metadata) {
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

    @JsonProperty("owner")
    public String getOwner() {
        return owner;
    }

    @JsonIgnore
    public void setOwner(String owner) {
        this.owner = owner;
    }

    @JsonProperty("account")
    public String getAccount() {
        return account;
    }

    @JsonIgnore
    public void setAccount(String account) {
        this.account = account;
    }

    @JsonProperty("public")
    public boolean isPublicInAccount() {
        return publicInAccount;
    }

    @JsonIgnore
    public void setPublicInAccount(boolean publicInAccount) {
        this.publicInAccount = publicInAccount;
    }
}
