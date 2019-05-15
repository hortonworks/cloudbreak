package com.sequenceiq.freeipa.api.v1.freeipa.stack.model.common.security;

import java.util.LinkedList;
import java.util.List;

import javax.validation.Valid;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.sequenceiq.freeipa.api.v1.freeipa.stack.doc.FreeIpaModelDescriptions.SecurityGroupModelDescription;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

@ApiModel("SecurityGroupV1Request")
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(Include.NON_NULL)
public class SecurityGroupRequest extends SecurityGroupBase {
    @Valid
    @ApiModelProperty(SecurityGroupModelDescription.SECURITY_RULES)
    private List<SecurityRuleRequest> securityRules = new LinkedList<>();

    public List<SecurityRuleRequest> getSecurityRules() {
        return securityRules;
    }

    public void setSecurityRules(List<SecurityRuleRequest> securityRules) {
        this.securityRules = securityRules;
    }
}
