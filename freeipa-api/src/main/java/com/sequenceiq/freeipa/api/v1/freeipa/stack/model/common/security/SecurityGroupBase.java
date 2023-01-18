package com.sequenceiq.freeipa.api.v1.freeipa.stack.model.common.security;

import java.util.Set;

import com.sequenceiq.freeipa.api.v1.freeipa.stack.doc.FreeIpaModelDescriptions.SecurityGroupModelDescription;

import io.swagger.v3.oas.annotations.media.Schema;

public abstract class SecurityGroupBase {
    @Schema(description = SecurityGroupModelDescription.SECURITY_GROUP_IDS)
    private Set<String> securityGroupIds;

    public Set<String> getSecurityGroupIds() {
        return securityGroupIds;
    }

    public void setSecurityGroupIds(Set<String> securityGroupIds) {
        this.securityGroupIds = securityGroupIds;
    }

    @Override
    public String toString() {
        return "SecurityGroupBase{"
                + "securityGroupIds=" + securityGroupIds
                + '}';
    }
}
