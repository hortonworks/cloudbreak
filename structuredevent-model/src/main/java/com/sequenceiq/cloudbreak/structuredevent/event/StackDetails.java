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
    private Long stackId;

    private String stackName;

    private String description;

    private String region;

    private String availabilityZone;

    private String cloudPlatform;

    private String platformVariant;

    private String stackStatus;

    private String detailedStackStatus;

    @JsonSerialize(using = Base64Serializer.class)
    @JsonDeserialize(using = Base64Deserializer.class)
    private String statusReason;

    private String cloudbreakVersion;

    private String imageIdentifier;

    private String ambariVersion;

    private String hdpVersion;

    private Boolean prewarmedImage;

    private Boolean existingNetwork;

    private Boolean exisitngSubnet;

    private List<InstanceGroupDetails> instanceGroups;

    public Long getStackId() {
        return stackId;
    }

    public void setStackId(Long stackId) {
        this.stackId = stackId;
    }

    public String getStackName() {
        return stackName;
    }

    public void setStackName(String stackName) {
        this.stackName = stackName;
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

    public String getStackStatus() {
        return stackStatus;
    }

    public void setStackStatus(String stackStatus) {
        this.stackStatus = stackStatus;
    }

    public String getDetailedStackStatus() {
        return detailedStackStatus;
    }

    public void setDetailedStackStatus(String detailedStackStatus) {
        this.detailedStackStatus = detailedStackStatus;
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

    public String getHdpVersion() {
        return hdpVersion;
    }

    public void setHdpVersion(String hdpVersion) {
        this.hdpVersion = hdpVersion;
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

    public Boolean getExisitngSubnet() {
        return exisitngSubnet;
    }

    public void setExisitngSubnet(Boolean exisitngSubnet) {
        this.exisitngSubnet = exisitngSubnet;
    }

    public List<InstanceGroupDetails> getInstanceGroups() {
        return instanceGroups;
    }

    public void setInstanceGroups(List<InstanceGroupDetails> instanceGroups) {
        this.instanceGroups = instanceGroups;
    }
}
