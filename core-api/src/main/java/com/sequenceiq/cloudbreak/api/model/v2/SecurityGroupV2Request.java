package com.sequenceiq.cloudbreak.api.model.v2;

import java.util.LinkedList;
import java.util.List;

import javax.validation.Valid;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.sequenceiq.cloudbreak.api.model.JsonEntity;
import com.sequenceiq.cloudbreak.api.model.SecurityRuleRequest;
import com.sequenceiq.cloudbreak.doc.ModelDescriptions.SecurityGroupModelDescription;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

@ApiModel
@JsonIgnoreProperties(ignoreUnknown = true)
public class SecurityGroupV2Request implements JsonEntity {

    @ApiModelProperty(SecurityGroupModelDescription.SECURITY_GROUP_ID)
    private String securityGroupId;

    @Valid
    @ApiModelProperty(SecurityGroupModelDescription.SECURITY_RULES)
    private List<SecurityRuleRequest> securityRules = new LinkedList<>();

    public List<SecurityRuleRequest> getSecurityRules() {
        return securityRules;
    }

    public void setSecurityRules(List<SecurityRuleRequest> securityRules) {
        this.securityRules = securityRules;
    }

    public String getSecurityGroupId() {
        return securityGroupId;
    }

    public void setSecurityGroupId(String securityGroupId) {
        this.securityGroupId = securityGroupId;
    }
}
