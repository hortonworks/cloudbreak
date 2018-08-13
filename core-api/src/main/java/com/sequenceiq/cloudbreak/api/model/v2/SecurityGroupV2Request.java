package com.sequenceiq.cloudbreak.api.model.v2;

import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import javax.validation.Valid;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.sequenceiq.cloudbreak.api.model.JsonEntity;
import com.sequenceiq.cloudbreak.api.model.SecurityRuleRequest;
import com.sequenceiq.cloudbreak.doc.ModelDescriptions.SecurityGroupModelDescription;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

@ApiModel
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(Include.NON_NULL)
public class SecurityGroupV2Request implements JsonEntity {

    /**
     * @deprecated in 2.8.0. We support multiple security groups.
     */
    @Deprecated(since = "2.8.0")
    @ApiModelProperty(SecurityGroupModelDescription.SECURITY_GROUP_ID)
    private String securityGroupId;

    @ApiModelProperty(SecurityGroupModelDescription.SECURITY_GROUP_IDS)
    private Set<String> securityGroupIds;

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

    public Set<String> getSecurityGroupIds() {
        return securityGroupIds;
    }

    public void setSecurityGroupIds(Set<String> securityGroupIds) {
        this.securityGroupIds = securityGroupIds;
    }
}
