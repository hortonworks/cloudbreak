package com.sequenceiq.cloudbreak.api.model;

import java.util.LinkedList;
import java.util.List;

import javax.validation.Valid;
import javax.validation.constraints.NotNull;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.sequenceiq.cloudbreak.doc.ModelDescriptions;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

@ApiModel
@JsonIgnoreProperties(ignoreUnknown = true)
public class SecurityGroupResponse extends SecurityGroupBase {

    @ApiModelProperty(value = ModelDescriptions.ID)
    private Long id;
    @ApiModelProperty(value = ModelDescriptions.OWNER)
    private String owner;
    @ApiModelProperty(value = ModelDescriptions.ACCOUNT)
    private String account;
    @Valid
    @ApiModelProperty(value = ModelDescriptions.SecurityGroupModelDescription.SECURITY_RULES)
    private List<SecurityRuleResponse> securityRules = new LinkedList<>();
    @ApiModelProperty(value = ModelDescriptions.PUBLIC_IN_ACCOUNT)
    @NotNull
    private boolean publicInAccount;

    public List<SecurityRuleResponse> getSecurityRules() {
        return securityRules;
    }

    public void setSecurityRules(List<SecurityRuleResponse> securityRules) {
        this.securityRules = securityRules;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
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
}
