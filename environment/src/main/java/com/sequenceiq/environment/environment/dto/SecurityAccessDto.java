package com.sequenceiq.environment.environment.dto;

import org.apache.commons.lang3.StringUtils;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonPOJOBuilder;
import com.sequenceiq.cloudbreak.validation.SubnetValidator;

@JsonDeserialize(builder = SecurityAccessDto.Builder.class)
public class SecurityAccessDto {

    private final String securityGroupIdForKnox;

    private final String defaultSecurityGroupId;

    private final String cidr;

    private final String securityAccessType;

    private SecurityAccessDto(Builder builder) {
        this.cidr = builder.cidr;
        this.securityGroupIdForKnox = builder.securityGroupIdForKnox;
        this.defaultSecurityGroupId = builder.defaultSecurityGroupId;
        this.securityAccessType = builder.securityAccessType;

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

    @JsonPOJOBuilder
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

        /**
         * Need this for Jackson deserialization
         * @param securityAccessType securityAccessType
         */
        private Builder withSecurityAccessType(String securityAccessType) {
            this.securityAccessType = securityAccessType;
            return this;
        }

        public SecurityAccessDto build() {
            determineAccessType();
            return new SecurityAccessDto(this);
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
