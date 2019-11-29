package com.sequenceiq.environment.environment.dto;

public class SecurityAccessDto {

    private String securityGroupIdForKnox;

    private String defaultSecurityGroupId;

    private String cidr;

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

    @Override
    public String toString() {
        return "SecurityAccessDto{" +
            "securityGroupIdForKnox='" + securityGroupIdForKnox + '\'' +
            ", defaultSecurityGroupId='" + defaultSecurityGroupId + '\'' +
            ", cidr='" + cidr + '\'' +
            '}';
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {

        private String securityGroupIdForKnox;

        private String defaultSecurityGroupId;

        private String cidr;

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
            SecurityAccessDto response = new SecurityAccessDto();
            response.cidr = cidr;
            response.securityGroupIdForKnox = securityGroupIdForKnox;
            response.defaultSecurityGroupId = defaultSecurityGroupId;
            return response;
        }
    }
}
