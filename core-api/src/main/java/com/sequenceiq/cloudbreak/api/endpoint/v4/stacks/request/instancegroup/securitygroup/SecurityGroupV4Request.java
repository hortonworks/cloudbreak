package com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.instancegroup.securitygroup;

import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import javax.validation.Valid;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.sequenceiq.cloudbreak.api.endpoint.v4.JsonEntity;
import com.sequenceiq.cloudbreak.api.endpoint.v4.util.requests.SecurityRuleV4Request;
import com.sequenceiq.cloudbreak.doc.ModelDescriptions.SecurityGroupModelDescription;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

@ApiModel
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(Include.NON_NULL)
public class SecurityGroupV4Request implements JsonEntity {

    @ApiModelProperty(SecurityGroupModelDescription.SECURITY_GROUP_IDS)
    private Set<String> securityGroupIds;

    @Valid
    @ApiModelProperty(SecurityGroupModelDescription.SECURITY_RULES)
    private List<SecurityRuleV4Request> securityRules = new LinkedList<>();

    public List<SecurityRuleV4Request> getSecurityRules() {
        return securityRules;
    }

    public void setSecurityRules(List<SecurityRuleV4Request> securityRules) {
        this.securityRules = securityRules;
    }

    public Set<String> getSecurityGroupIds() {
        return securityGroupIds;
    }

    public void setSecurityGroupIds(Set<String> securityGroupIds) {
        this.securityGroupIds = securityGroupIds;
    }
}
