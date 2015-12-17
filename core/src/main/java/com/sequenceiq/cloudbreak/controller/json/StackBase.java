package com.sequenceiq.cloudbreak.controller.json;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Pattern;
import javax.validation.constraints.Size;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.sequenceiq.cloudbreak.common.type.OnFailureAction;
import com.sequenceiq.cloudbreak.controller.doc.ModelDescriptions;
import com.sequenceiq.cloudbreak.controller.doc.ModelDescriptions.StackModelDescription;
import com.wordnik.swagger.annotations.ApiModelProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
public abstract class StackBase implements JsonEntity {
    @Size(max = 40, min = 5, message = "The length of the name has to be in range of 5 to 40")
    @Pattern(regexp = "([a-z][-a-z0-9]*[a-z0-9])",
            message = "The name can only contain lowercase alphanumeric characters and hyphens and has start with an alphanumeric character")
    @NotNull
    @ApiModelProperty(value = StackModelDescription.STACK_NAME, required = true)
    private String name;
    @ApiModelProperty(value = StackModelDescription.AVAILABILITY_ZONE, required = false)
    private String availabilityZone;
    @NotNull
    @ApiModelProperty(value = StackModelDescription.REGION, required = true)
    private String region;
    @ApiModelProperty(value = ModelDescriptions.CLOUD_PLATFORM, required = true)
    private String cloudPlatform;
    @ApiModelProperty(StackModelDescription.PLATFORM_VARIANT)
    private String platformVariant;
    @NotNull
    @ApiModelProperty(value = StackModelDescription.CREDENTIAL_ID, required = true)
    private Long credentialId;
    @ApiModelProperty(StackModelDescription.FAILURE_ACTION)
    private OnFailureAction onFailureAction = OnFailureAction.ROLLBACK;
    @ApiModelProperty(StackModelDescription.FAILURE_POLICY)
    private FailurePolicyJson failurePolicy;
    @Valid
    @ApiModelProperty(required = true)
    private List<InstanceGroupJson> instanceGroups = new ArrayList<>();
    @NotNull
    @ApiModelProperty(value = StackModelDescription.SECURITY_GROUP_ID, required = true)
    private Long securityGroupId;
    @NotNull
    @ApiModelProperty(value = StackModelDescription.NETWORK_ID, required = true)
    private Long networkId;
    @ApiModelProperty(StackModelDescription.PARAMETERS)
    private Map<String, String> parameters = new HashMap<>();

    public FailurePolicyJson getFailurePolicy() {
        return failurePolicy;
    }

    public void setFailurePolicy(FailurePolicyJson failurePolicy) {
        this.failurePolicy = failurePolicy;
    }

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

    public List<InstanceGroupJson> getInstanceGroups() {
        return instanceGroups;
    }

    public void setInstanceGroups(List<InstanceGroupJson> instanceGroups) {
        this.instanceGroups = instanceGroups;
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

    public Long getSecurityGroupId() {
        return securityGroupId;
    }

    public void setSecurityGroupId(Long securityGroupId) {
        this.securityGroupId = securityGroupId;
    }

    public String getPlatformVariant() {
        return platformVariant;
    }

    public void setPlatformVariant(String platformVariant) {
        this.platformVariant = platformVariant;
    }
}
