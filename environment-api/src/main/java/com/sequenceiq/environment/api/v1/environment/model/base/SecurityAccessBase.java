package com.sequenceiq.environment.api.v1.environment.model.base;

import java.lang.reflect.InvocationTargetException;

import javax.validation.constraints.Size;

import com.sequenceiq.cloudbreak.validation.MutuallyExclusiveNotNull;
import com.sequenceiq.environment.api.doc.environment.EnvironmentModelDescription;
import com.sequenceiq.environment.api.v1.environment.validator.cidr.ValidCidrList;

import io.swagger.annotations.ApiModelProperty;

@MutuallyExclusiveNotNull(fieldGroups = {"securityGroupIdForKnox,defaultSecurityGroupId", "cidr"},
        message = "Please set either only the CIDR field or both security group id fields")
public abstract class SecurityAccessBase {

    @Size(max = 255, message = "The length of the security group ID can be maximum 255 characters.")
    @ApiModelProperty(EnvironmentModelDescription.KNOX_SECURITY_GROUP)
    private String securityGroupIdForKnox;

    @Size(max = 255, message = "The length of the security group ID can be maximum 255 characters.")
    @ApiModelProperty(EnvironmentModelDescription.DEFAULT_SECURITY_GROUP)
    private String defaultSecurityGroupId;

    @ValidCidrList
    @Size(min = 5, max = 255, message = "The list of CIDRs must consist of characters between 9 and 255")
    @ApiModelProperty(EnvironmentModelDescription.SECURITY_CIDR)
    private String cidr;

    public String getSecurityGroupIdForKnox() {
        return securityGroupIdForKnox;
    }

    public void setSecurityGroupIdForKnox(String securityGroupIdForKnox) {
        this.securityGroupIdForKnox = securityGroupIdForKnox;
    }

    public String getDefaultSecurityGroupId() {
        return defaultSecurityGroupId;
    }

    public void setDefaultSecurityGroupId(String defaultSecurityGroupId) {
        this.defaultSecurityGroupId = defaultSecurityGroupId;
    }

    public String getCidr() {
        return cidr;
    }

    public void setCidr(String cidr) {
        this.cidr = cidr;
    }

    public static class Builder<T extends SecurityAccessBase> {

        private final Class<T> baseClass;

        private String securityGroupIdForKnox;

        private String defaultSecurityGroupId;

        private String cidr;

        public Builder(Class<T> baseClass) {
            this.baseClass = baseClass;
        }

        public Builder<T> withSecurityGroupIdForKnox(String securityGroupIdForKnox) {
            this.securityGroupIdForKnox = securityGroupIdForKnox;
            return this;
        }

        public Builder<T> withDefaultSecurityGroupId(String defaultSecurityGroupId) {
            this.defaultSecurityGroupId = defaultSecurityGroupId;
            return this;
        }

        public Builder<T> withCidr(String cidr) {
            this.cidr = cidr;
            return this;
        }

        public T build() {
            T response;
            try {
                response = baseClass.getDeclaredConstructor().newInstance();
            } catch (InstantiationException | IllegalAccessException | InvocationTargetException | NoSuchMethodException e) {
                throw new RuntimeException("Failed to instantiate " + baseClass.getCanonicalName(), e);
            }
            response.setCidr(cidr);
            response.setSecurityGroupIdForKnox(securityGroupIdForKnox);
            response.setDefaultSecurityGroupId(defaultSecurityGroupId);
            return response;
        }
    }
}
