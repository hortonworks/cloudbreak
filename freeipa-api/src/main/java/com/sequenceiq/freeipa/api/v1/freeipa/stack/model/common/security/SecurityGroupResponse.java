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

@ApiModel("SecurityGroupV1Response")
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(Include.NON_NULL)
public class SecurityGroupResponse extends SecurityGroupBase {
    @Valid
    @ApiModelProperty(SecurityGroupModelDescription.SECURITY_RULES)
    private List<SecurityRuleResponse> securityRules = new LinkedList<>();

    public List<SecurityRuleResponse> getSecurityRules() {
        return securityRules;
    }

    public void setSecurityRules(List<SecurityRuleResponse> securityRules) {
        this.securityRules = securityRules;
    }

    @Override
    public String toString() {
        return "SecurityGroupResponse{" +
                "SecurityGroupBase=" + super.toString() +
                ", securityRules=" + securityRules +
                '}';
    }
}
