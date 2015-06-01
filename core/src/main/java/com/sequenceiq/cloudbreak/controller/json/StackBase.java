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
import com.sequenceiq.cloudbreak.controller.doc.ModelDescriptions;
import com.sequenceiq.cloudbreak.controller.doc.ModelDescriptions.StackModelDescription;
import com.sequenceiq.cloudbreak.domain.CloudPlatform;
import com.sequenceiq.cloudbreak.domain.OnFailureAction;
import com.wordnik.swagger.annotations.ApiModelProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
public abstract class StackBase implements JsonEntity {
    @Size(max = 40, min = 5, message = "The length of the name has to be in range of 5 to 40")
    @Pattern(regexp = "([a-z][-a-z0-9]*[a-z0-9])",
            message = "The name can only contain lowercase alphanumeric characters and hyphens and has start with an alphanumeric character")
    @NotNull
    @ApiModelProperty(value = StackModelDescription.STACK_NAME, required = true)
    private String name;
    @NotNull
    @ApiModelProperty(value = StackModelDescription.REGION, required = true)
    private String region;
    @ApiModelProperty(value = ModelDescriptions.CLOUD_PLATFORM, required = true)
    private CloudPlatform cloudPlatform;
    @NotNull
    @ApiModelProperty(value = StackModelDescription.CREDENTIAL_ID, required = true)
    private Long credentialId;
    @ApiModelProperty(value = StackModelDescription.IMAGE, required = true)
    private String image;
    @ApiModelProperty(StackModelDescription.FAILURE_ACTION)
    private OnFailureAction onFailureAction = OnFailureAction.ROLLBACK;
    @ApiModelProperty(StackModelDescription.FAILURE_POLICY)
    private FailurePolicyJson failurePolicy;
    @Valid
    @ApiModelProperty(required = true)
    private List<InstanceGroupJson> instanceGroups = new ArrayList<>();
    @ApiModelProperty(StackModelDescription.ALLOWED_SUBNETS)
    private List<SubnetJson> allowedSubnets = new ArrayList<>();
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

    public String getRegion() {
        return region;
    }

    public void setRegion(String region) {
        this.region = region;
    }

    public List<SubnetJson> getAllowedSubnets() {
        return allowedSubnets;
    }

    public void setAllowedSubnets(List<SubnetJson> allowedSubnets) {
        this.allowedSubnets = allowedSubnets;
    }

    public String getImage() {
        return image;
    }

    public void setImage(String image) {
        this.image = image;
    }

    public Long getNetworkId() {
        return networkId;
    }

    public void setNetworkId(Long networkId) {
        this.networkId = networkId;
    }

    public Map<String, String> getParameters() {
        return parameters;
    }

    public void setParameters(Map<String, String> parameters) {
        this.parameters = parameters;
    }
}
