package com.sequenceiq.cloudbreak.api.model;

import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.sequenceiq.cloudbreak.doc.ModelDescriptions;
import com.sequenceiq.cloudbreak.doc.ModelDescriptions.SecurityGroupModelDescription;

import io.swagger.annotations.ApiModelProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
public abstract class SecurityGroupBase implements JsonEntity {

    @ApiModelProperty(ModelDescriptions.DESCRIPTION)
    @Size(max = 1000)
    private String description;

    @ApiModelProperty(SecurityGroupModelDescription.SECURITY_GROUP_ID)
    private String securityGroupId;

    @ApiModelProperty(value = ModelDescriptions.CLOUD_PLATFORM, required = true)
    @NotNull
    private String cloudPlatform;

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getSecurityGroupId() {
        return securityGroupId;
    }

    public void setSecurityGroupId(String securityGroupId) {
        this.securityGroupId = securityGroupId;
    }

    public String getCloudPlatform() {
        return cloudPlatform;
    }

    public void setCloudPlatform(String cloudPlatform) {
        this.cloudPlatform = cloudPlatform;
    }
}
