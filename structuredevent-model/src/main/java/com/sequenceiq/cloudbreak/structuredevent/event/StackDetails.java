package com.sequenceiq.cloudbreak.structuredevent.event;

import java.io.Serializable;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.sequenceiq.cloudbreak.structuredevent.json.Base64Deserializer;
import com.sequenceiq.cloudbreak.structuredevent.json.Base64Serializer;

@JsonIgnoreProperties(ignoreUnknown = true)
public class StackDetails implements Serializable {
    private Long id;

    private String name;

    private String type;

    private String description;

    private String region;

    private String availabilityZone;

    private String cloudPlatform;

    private String platformVariant;

    private String status;

    private String detailedStatus;

    @JsonSerialize(using = Base64Serializer.class)
    @JsonDeserialize(using = Base64Deserializer.class)
    private String statusReason;

    private String cloudbreakVersion;

    private String imageIdentifier;

    private String ambariVersion;

    private String clusterType;

    private String clusterVersion;

    private Boolean prewarmedImage;

    private Boolean existingNetwork;

    private Boolean existingSubnet;

    private List<InstanceGroupDetails> instanceGroups;

    private Long datalakeId;

    private Long datalakeResourceId;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getRegion() {
        return region;
    }

    public void setRegion(String region) {
        this.region = region;
    }

    public String getAvailabilityZone() {
        return availabilityZone;
    }

    public void setAvailabilityZone(String availabilityZone) {
        this.availabilityZone = availabilityZone;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getCloudPlatform() {
        return cloudPlatform;
    }

    public void setCloudPlatform(String cloudPlatform) {
        this.cloudPlatform = cloudPlatform;
    }

    public String getPlatformVariant() {
        return platformVariant;
    }

    public void setPlatformVariant(String platformVariant) {
        this.platformVariant = platformVariant;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getDetailedStatus() {
        return detailedStatus;
    }

    public void setDetailedStatus(String detailedStatus) {
        this.detailedStatus = detailedStatus;
    }

    public String getStatusReason() {
        return statusReason;
    }

    public void setStatusReason(String statusReason) {
        this.statusReason = statusReason;
    }

    public String getCloudbreakVersion() {
        return cloudbreakVersion;
    }

    public void setCloudbreakVersion(String cloudbreakVersion) {
        this.cloudbreakVersion = cloudbreakVersion;
    }

    public String getImageIdentifier() {
        return imageIdentifier;
    }

    public void setImageIdentifier(String imageIdentifier) {
        this.imageIdentifier = imageIdentifier;
    }

    public String getAmbariVersion() {
        return ambariVersion;
    }

    public void setAmbariVersion(String ambariVersion) {
        this.ambariVersion = ambariVersion;
    }

    public String getClusterType() {
        return clusterType;
    }

    public void setClusterType(String clusterType) {
        this.clusterType = clusterType;
    }

    public String getClusterVersion() {
        return clusterVersion;
    }

    public void setClusterVersion(String clusterVersion) {
        this.clusterVersion = clusterVersion;
    }

    public Boolean getPrewarmedImage() {
        return prewarmedImage;
    }

    public void setPrewarmedImage(Boolean prewarmedImage) {
        this.prewarmedImage = prewarmedImage;
    }

    public Boolean getExistingNetwork() {
        return existingNetwork;
    }

    public void setExistingNetwork(Boolean existingNetwork) {
        this.existingNetwork = existingNetwork;
    }

    public Boolean getExistingSubnet() {
        return existingSubnet;
    }

    public void setExistingSubnet(Boolean existingSubnet) {
        this.existingSubnet = existingSubnet;
    }

    public List<InstanceGroupDetails> getInstanceGroups() {
        return instanceGroups;
    }

    public void setInstanceGroups(List<InstanceGroupDetails> instanceGroups) {
        this.instanceGroups = instanceGroups;
    }

    public Long getDatalakeId() {
        return datalakeId;
    }

    public void setDatalakeId(Long datalakeId) {
        this.datalakeId = datalakeId;
    }

    public Long getDatalakeResourceId() {
        return datalakeResourceId;
    }

    public void setDatalakeResourceId(Long datalakeResourceId) {
        this.datalakeResourceId = datalakeResourceId;
    }
}
