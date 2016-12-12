package com.sequenceiq.cloudbreak.api.model;

import java.util.LinkedList;
import java.util.List;

import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Pattern;
import javax.validation.constraints.Size;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.sequenceiq.cloudbreak.doc.ModelDescriptions;

import io.swagger.annotations.ApiModelProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
public class SecurityGroupJson {
    @ApiModelProperty(value = ModelDescriptions.ID)
    private Long id;
    @ApiModelProperty(value = ModelDescriptions.NAME, required = true)
    @Size(max = 100, min = 1, message = "The length of the security group's name has to be in range of 1 to 100")
    @Pattern(regexp = "([a-z][-a-z0-9]*[a-z0-9])",
            message = "The security group's name can only contain lowercase alphanumeric characters and hyphens and has start with an alphanumeric character")
    @NotNull
    private String name;
    @ApiModelProperty(value = ModelDescriptions.OWNER)
    private String owner;
    @ApiModelProperty(value = ModelDescriptions.ACCOUNT)
    private String account;
    @ApiModelProperty(value = ModelDescriptions.PUBLIC_IN_ACCOUNT)
    @NotNull
    private boolean publicInAccount;
    @ApiModelProperty(value = ModelDescriptions.DESCRIPTION)
    @Size(max = 1000)
    private String description;
    @Valid
    @ApiModelProperty(value = ModelDescriptions.SecurityGroupModelDescription.SECURITY_RULES, required = true)
    private List<SecurityRuleJson> securityRules = new LinkedList<>();
    @ApiModelProperty(value = ModelDescriptions.SecurityGroupModelDescription.SECURITY_GROUP_ID)
    private String securityGroupId;
    @ApiModelProperty(value = ModelDescriptions.CLOUD_PLATFORM, required = true)
    @NotNull
    private String cloudPlatform;

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

    public String getOwner() {
        return owner;
    }

    public void setOwner(String owner) {
        this.owner = owner;
    }

    public String getAccount() {
        return account;
    }

    public void setAccount(String account) {
        this.account = account;
    }

    public boolean isPublicInAccount() {
        return publicInAccount;
    }

    public void setPublicInAccount(boolean publicInAccount) {
        this.publicInAccount = publicInAccount;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public List<SecurityRuleJson> getSecurityRules() {
        return securityRules;
    }

    public void setSecurityRules(List<SecurityRuleJson> securityRules) {
        this.securityRules = securityRules;
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
