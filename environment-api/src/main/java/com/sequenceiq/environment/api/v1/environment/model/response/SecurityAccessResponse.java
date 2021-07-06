package com.sequenceiq.environment.api.v1.environment.model.response;

import java.util.Set;

import com.sequenceiq.environment.api.doc.environment.EnvironmentModelDescription;
import com.sequenceiq.environment.api.v1.environment.model.base.SecurityAccessBase;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

@ApiModel("SecurityAccessV1Response")
public class SecurityAccessResponse extends SecurityAccessBase {

    @ApiModelProperty(EnvironmentModelDescription.KNOX_SECURITY_GROUPS)
    private Set<String> securityGroupIdsForKnox;

    @ApiModelProperty(EnvironmentModelDescription.DEFAULT_SECURITY_GROUPS)
    private Set<String> defaultSecurityGroupIds;

    public Set<String> getSecurityGroupIdsForKnox() {
        return securityGroupIdsForKnox;
    }

    public void setSecurityGroupIdsForKnox(Set<String> securityGroupIdsForKnox) {
        this.securityGroupIdsForKnox = securityGroupIdsForKnox;
    }

    public Set<String> getDefaultSecurityGroupIds() {
        return defaultSecurityGroupIds;
    }

    public void setDefaultSecurityGroupIds(Set<String> defaultSecurityGroupIds) {
        this.defaultSecurityGroupIds = defaultSecurityGroupIds;
    }

    @Override
    public String toString() {
        return "SecurityAccessResponse{" +
                "securityGroupIdsForKnox=" + securityGroupIdsForKnox +
                ", defaultSecurityGroupIds=" + defaultSecurityGroupIds +
                '}';
    }

    public static SecurityAccessResponse.Builder builder() {
        return new SecurityAccessResponse.Builder();
    }

    public static final class Builder {

        private String securityGroupIdForKnox;

        private String defaultSecurityGroupId;

        private String cidr;

        private Set<String> securityGroupIdsForKnox;

        private Set<String> defaultSecurityGroupIds;

        private Builder() {
        }

        public Builder withDefaultSecurityGroupIds(Set<String> defaultSecurityGroupIds) {
            this.defaultSecurityGroupIds = defaultSecurityGroupIds;
            return this;
        }

        public Builder withSecurityGroupIdsForKnox(Set<String> securityGroupIdsForKnox) {
            this.securityGroupIdsForKnox = securityGroupIdsForKnox;
            return this;
        }

        public Builder withSecurityGroupIdForKnox(String securityGroupIdForKnox) {
            this.securityGroupIdForKnox = securityGroupIdForKnox;
            return this;
        }

        public Builder withDefaultSecurityGroupId(String defaultSecurityGroupId) {
            this.defaultSecurityGroupId = defaultSecurityGroupId;
            return this;
        }

        public Builder withCidr(String cidr) {
            this.cidr = cidr;
            return this;
        }

        public SecurityAccessResponse build() {
            SecurityAccessResponse securityAccessResponse = new SecurityAccessResponse();
            securityAccessResponse.setCidr(cidr);
            securityAccessResponse.setDefaultSecurityGroupIds(defaultSecurityGroupIds);
            securityAccessResponse.setDefaultSecurityGroupId(defaultSecurityGroupId);
            securityAccessResponse.setSecurityGroupIdsForKnox(securityGroupIdsForKnox);
            securityAccessResponse.setSecurityGroupIdForKnox(securityGroupIdForKnox);
            return securityAccessResponse;
        }
    }
}
