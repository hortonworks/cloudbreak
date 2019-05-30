package com.sequenceiq.cloudbreak.dto.credential.azure;

import java.io.Serializable;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

@ApiModel("RoleBasedV1Response")
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(Include.NON_NULL)
public class AzureRoleBasedAttributes implements Serializable {

    @ApiModelProperty
    private final String roleName;

    @ApiModelProperty(hidden = true)
    private final String deploymentAddress;

    @ApiModelProperty
    private final String spDisplayName;

    @ApiModelProperty
    private final Boolean codeGrantFlow;

    @ApiModelProperty
    private final String appObjectId;

    private AzureRoleBasedAttributes(Builder builder) {
        roleName = builder.roleName;
        deploymentAddress = builder.deploymentAddress;
        spDisplayName = builder.spDisplayName;
        codeGrantFlow = builder.codeGrantFlow;
        appObjectId = builder.appObjectId;
    }

    public String getRoleName() {
        return roleName;
    }

    public String getDeploymentAddress() {
        return deploymentAddress;
    }

    public String getSpDisplayName() {
        return spDisplayName;
    }

    public Boolean getCodeGrantFlow() {
        return codeGrantFlow;
    }

    public String getAppObjectId() {
        return appObjectId;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static final class Builder {
        private String roleName;

        private String deploymentAddress;

        private String spDisplayName;

        private Boolean codeGrantFlow;

        private String appObjectId;

        public Builder roleName(String roleName) {
            this.roleName = roleName;
            return this;
        }

        public Builder deploymentAddress(String deploymentAddress) {
            this.deploymentAddress = deploymentAddress;
            return this;
        }

        public Builder spDisplayName(String spDisplayName) {
            this.spDisplayName = spDisplayName;
            return this;
        }

        public Builder codeGrantFlow(Boolean codeGrantFlow) {
            this.codeGrantFlow = codeGrantFlow;
            return this;
        }

        public Builder appObjectId(String appObjectId) {
            this.appObjectId = appObjectId;
            return this;
        }

        public AzureRoleBasedAttributes build() {
            return new AzureRoleBasedAttributes(this);
        }
    }
}
