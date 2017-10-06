package com.sequenceiq.cloudbreak.api.model;

import java.util.HashMap;
import java.util.Map;

import javax.validation.constraints.NotNull;
import javax.validation.constraints.Pattern;
import javax.validation.constraints.Size;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.sequenceiq.cloudbreak.doc.ModelDescriptions;
import com.sequenceiq.cloudbreak.doc.ModelDescriptions.StackModelDescription;

import io.swagger.annotations.ApiModelProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
public abstract class StackBase implements JsonEntity {
    @Size(max = 40, min = 5, message = "The length of the name has to be in range of 5 to 40")
    @Pattern(regexp = "([a-z][-a-z0-9]*[a-z0-9])",
            message = "The name can only contain lowercase alphanumeric characters and hyphens and has start with an alphanumeric character")
    @NotNull
    @ApiModelProperty(value = StackModelDescription.STACK_NAME, required = true)
    private String name;

    @ApiModelProperty(StackModelDescription.AVAILABILITY_ZONE)
    private String availabilityZone;

    @ApiModelProperty(StackModelDescription.REGION)
    private String region;

    @ApiModelProperty(ModelDescriptions.CLOUD_PLATFORM)
    private String cloudPlatform;

    @ApiModelProperty(StackModelDescription.PLATFORM_VARIANT)
    private String platformVariant;

    @ApiModelProperty(StackModelDescription.CREDENTIAL_ID)
    private Long credentialId;

    @ApiModelProperty(StackModelDescription.FAILURE_ACTION)
    private OnFailureAction onFailureAction = OnFailureAction.DO_NOTHING;

    @ApiModelProperty(StackModelDescription.NETWORK_ID)
    private Long networkId;

    @ApiModelProperty(StackModelDescription.AMBARI_VERSION)
    private String ambariVersion;

    @ApiModelProperty(StackModelDescription.HDP_VERSION)
    private String hdpVersion;

    @ApiModelProperty(StackModelDescription.PARAMETERS)
    private Map<String, String> parameters = new HashMap<>();

    @ApiModelProperty(StackModelDescription.CUSTOM_DOMAIN)
    private String customDomain;

    @ApiModelProperty(StackModelDescription.CUSTOM_HOSTNAME)
    private String customHostname;

    @ApiModelProperty(StackModelDescription.CLUSTER_NAME_AS_SUBDOMAIN)
    private boolean clusterNameAsSubdomain;

    @ApiModelProperty(StackModelDescription.HOSTGROUP_NAME_AS_HOSTNAME)
    private boolean hostgroupNameAsHostname;

    @ApiModelProperty(StackModelDescription.APPLICATION_TAGS)
    private Map<String, String> applicationTags = new HashMap<>();

    @ApiModelProperty(StackModelDescription.USERDEFINED_TAGS)
    private Map<String, String> userDefinedTags = new HashMap<>();

    @ApiModelProperty(StackModelDescription.DEFAULT_TAGS)
    private Map<String, String> defaultTags = new HashMap<>();

    public OnFailureAction getOnFailureAction() {
        return onFailureAction;
    }

    public void setOnFailureAction(OnFailureAction onFailureAction) {
        this.onFailureAction = onFailureAction;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    @JsonProperty("cloudPlatform")
    public String getCloudPlatform() {
        return cloudPlatform;
    }

    @JsonIgnore
    public void setCloudPlatform(String cloudPlatform) {
        this.cloudPlatform = cloudPlatform;
    }

    public Long getCredentialId() {
        return credentialId;
    }

    public void setCredentialId(Long credentialId) {
        this.credentialId = credentialId;
    }

    public String getRegion() {
        return region;
    }

    public void setRegion(String region) {
        this.region = region;
    }

    public Long getNetworkId() {
        return networkId;
    }

    public void setNetworkId(Long networkId) {
        this.networkId = networkId;
    }

    public String getAvailabilityZone() {
        return availabilityZone;
    }

    public void setAvailabilityZone(String availabilityZone) {
        this.availabilityZone = availabilityZone;
    }

    public Map<String, String> getParameters() {
        return parameters;
    }

    public void setParameters(Map<String, String> parameters) {
        this.parameters = parameters;
    }

    public String getPlatformVariant() {
        return platformVariant;
    }

    public void setPlatformVariant(String platformVariant) {
        this.platformVariant = platformVariant;
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

    public String getCustomDomain() {
        return customDomain;
    }

    public void setCustomDomain(String customDomain) {
        this.customDomain = customDomain;
    }

    public String getCustomHostname() {
        return customHostname;
    }

    public void setCustomHostname(String customHostname) {
        this.customHostname = customHostname;
    }

    public boolean isClusterNameAsSubdomain() {
        return clusterNameAsSubdomain;
    }

    public void setClusterNameAsSubdomain(boolean clusterNameAsSubdomain) {
        this.clusterNameAsSubdomain = clusterNameAsSubdomain;
    }

    public boolean isHostgroupNameAsHostname() {
        return hostgroupNameAsHostname;
    }

    public void setHostgroupNameAsHostname(boolean hostgroupNameAsHostname) {
        this.hostgroupNameAsHostname = hostgroupNameAsHostname;
    }

    public Map<String, String> getApplicationTags() {
        return applicationTags;
    }

    public void setApplicationTags(Map<String, String> applicationTags) {
        this.applicationTags = applicationTags;
    }

    public Map<String, String> getUserDefinedTags() {
        return userDefinedTags;
    }

    public void setUserDefinedTags(Map<String, String> userDefinedTags) {
        this.userDefinedTags = userDefinedTags;
    }

    public Map<String, String> getDefaultTags() {
        return defaultTags;
    }

    public void setDefaultTags(Map<String, String> defaultTags) {
        this.defaultTags = defaultTags;
    }

}
