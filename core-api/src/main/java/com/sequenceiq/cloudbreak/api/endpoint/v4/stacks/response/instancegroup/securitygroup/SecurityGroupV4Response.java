package com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.instancegroup.securitygroup;

import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import jakarta.validation.Valid;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.sequenceiq.cloudbreak.api.endpoint.v4.util.responses.SecurityRuleV4Response;
import com.sequenceiq.cloudbreak.doc.ModelDescriptions.SecurityGroupModelDescription;
import com.sequenceiq.common.model.JsonEntity;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(Include.NON_NULL)
public class SecurityGroupV4Response implements JsonEntity {

    @Schema(description = SecurityGroupModelDescription.SECURITY_GROUP_IDS, requiredMode = Schema.RequiredMode.REQUIRED)
    private Set<String> securityGroupIds = new HashSet<>();

    @Valid
    @Schema(description = SecurityGroupModelDescription.SECURITY_RULES, requiredMode = Schema.RequiredMode.REQUIRED)
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
