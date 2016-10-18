package com.sequenceiq.cloudbreak.api.model;

import java.util.LinkedList;
import java.util.List;

import javax.validation.Valid;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.sequenceiq.cloudbreak.doc.ModelDescriptions;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

@ApiModel
@JsonIgnoreProperties(ignoreUnknown = true)
public class SecurityGroupRequest extends SecurityGroupBase {

    @Valid
    @ApiModelProperty(value = ModelDescriptions.SecurityGroupModelDescription.SECURITY_RULES)
    private List<SecurityRuleRequest> securityRules = new LinkedList<>();

    public List<SecurityRuleRequest> getSecurityRules() {
        return securityRules;
    }

    public void setSecurityRules(List<SecurityRuleRequest> securityRules) {
        this.securityRules = securityRules;
    }
}
