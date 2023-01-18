package com.sequenceiq.redbeams.api.endpoint.v4.stacks;

import java.util.Set;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.sequenceiq.redbeams.doc.ModelDescriptions.SecurityGroupModelDescriptions;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(Include.NON_NULL)
public class SecurityGroupV4StackRequest {

    @Schema(description = SecurityGroupModelDescriptions.SECURITY_GROUP_IDS)
    private Set<String> securityGroupIds;

    public Set<String> getSecurityGroupIds() {
        return securityGroupIds;
    }

    public void setSecurityGroupIds(Set<String> securityGroupIds) {
        this.securityGroupIds = securityGroupIds;
    }

    @Override
    public String toString() {
        return "SecurityGroupV4StackRequest{" +
                "securityGroupIds=" + securityGroupIds +
                '}';
    }
}
