package com.sequenceiq.environment.environment.dto;

import org.apache.commons.lang3.StringUtils;

import com.sequenceiq.cloudbreak.validation.SubnetValidator;

public class SecurityAccessDto {

    private String securityGroupIdForKnox;

    private String defaultSecurityGroupId;

    private String cidr;

    private String securityAccessType;

    private SecurityAccessDto() {
    }

    public String getSecurityGroupIdForKnox() {
        return securityGroupIdForKnox;
    }

    public String getDefaultSecurityGroupId() {
        return defaultSecurityGroupId;
    }

    public String getCidr() {
        return cidr;
    }

    public String getSecurityAccessType() {
        return securityAccessType;
    }

    @Override
    public String toString() {
        return "SecurityAccessDto{" +
                "securityGroupIdForKnox='" + securityGroupIdForKnox + '\'' +
                ", defaultSecurityGroupId='" + defaultSecurityGroupId + '\'' +
                ", cidr='" + cidr + '\'' +
                ", securityAccessType='" + securityAccessType + '\'' +
                '}';
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {

        private String securityGroupIdForKnox;

        private String defaultSecurityGroupId;

        private String cidr;

        private String securityAccessType;

        private Builder() {
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

        public SecurityAccessDto build() {
            determineAccessType();

            SecurityAccessDto response = new SecurityAccessDto();
            response.cidr = cidr;
            response.securityGroupIdForKnox = securityGroupIdForKnox;
            response.defaultSecurityGroupId = defaultSecurityGroupId;
            response.securityAccessType = securityAccessType;
            return response;
        }

        private void determineAccessType() {
            if (StringUtils.isNoneEmpty(cidr)) {
                if (cidr.contains("0.0.0.0/0")) {
                    securityAccessType = "CIDR_WIDE_OPEN";
                } else {
                    SubnetValidator subnetValidator = new SubnetValidator();
                    if (subnetValidator.isValid(cidr, null)) {
                        securityAccessType = "CIDR_PRIVATE";
                    } else {
                        securityAccessType = "CIDR_PUBLIC";
                    }
                }
            } else if (StringUtils.isNoneEmpty(defaultSecurityGroupId) && StringUtils.isNoneEmpty(securityGroupIdForKnox)) {
                securityAccessType = "EXISTING_SECURITY_GROUP";
            }
        }
    }
}
