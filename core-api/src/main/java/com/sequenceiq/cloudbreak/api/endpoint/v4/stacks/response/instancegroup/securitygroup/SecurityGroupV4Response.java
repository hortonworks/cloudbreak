package com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.instancegroup.securitygroup;

import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import javax.validation.Valid;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.sequenceiq.common.model.JsonEntity;
import com.sequenceiq.cloudbreak.api.endpoint.v4.util.responses.SecurityRuleV4Response;
import com.sequenceiq.cloudbreak.doc.ModelDescriptions.SecurityGroupModelDescription;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

@ApiModel
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(Include.NON_NULL)
public class SecurityGroupV4Response implements JsonEntity {

    @ApiModelProperty(SecurityGroupModelDescription.SECURITY_GROUP_IDS)
    private Set<String> securityGroupIds;

    @Valid
    @ApiModelProperty(SecurityGroupModelDescription.SECURITY_RULES)
    private List<SecurityRuleV4Response> securityRules = new LinkedList<>();

    public List<SecurityRuleV4Response> getSecurityRules() {
        return securityRules;
    }

    public void setSecurityRules(List<SecurityRuleV4Response> securityRules) {
        this.securityRules = securityRules;
    }

    public Set<String> getSecurityGroupIds() {
        return securityGroupIds;
    }

    public void setSecurityGroupIds(Set<String> securityGroupIds) {
        this.securityGroupIds = securityGroupIds;
    }
}
