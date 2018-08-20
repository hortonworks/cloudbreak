package com.sequenceiq.cloudbreak.api.model;

import java.util.LinkedList;
import java.util.List;

import javax.validation.Valid;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.sequenceiq.cloudbreak.api.model.users.OrganizationResourceResponse;
import com.sequenceiq.cloudbreak.doc.ModelDescriptions;
import com.sequenceiq.cloudbreak.doc.ModelDescriptions.SecurityGroupModelDescription;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

@ApiModel
@JsonIgnoreProperties(ignoreUnknown = true)
public class SecurityGroupResponse extends SecurityGroupBase {

    @ApiModelProperty(ModelDescriptions.NAME)
    private String name;

    @ApiModelProperty(ModelDescriptions.ID)
    private Long id;

    @Valid
    @ApiModelProperty(SecurityGroupModelDescription.SECURITY_RULES)
    private List<SecurityRuleResponse> securityRules = new LinkedList<>();

    @ApiModelProperty(ModelDescriptions.ORGANIZATION_OF_THE_RESOURCE)
    private OrganizationResourceResponse organization;

    public OrganizationResourceResponse getOrganization() {
        return organization;
    }

    public void setOrganization(OrganizationResourceResponse organization) {
        this.organization = organization;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

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

}
