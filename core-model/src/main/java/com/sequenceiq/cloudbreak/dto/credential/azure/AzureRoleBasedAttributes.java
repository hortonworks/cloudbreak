package com.sequenceiq.cloudbreak.dto.credential.azure;

import java.io.Serializable;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(name = "RoleBasedV1Response")
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(Include.NON_NULL)
public class AzureRoleBasedAttributes implements Serializable {

    @Schema(hidden = true)
    private final String deploymentAddress;

    @Schema
    private final String spDisplayName;

    @Schema
    private final Boolean codeGrantFlow;

    @Schema
    private final String appObjectId;

    private AzureRoleBasedAttributes(Builder builder) {
        deploymentAddress = builder.deploymentAddress;
        spDisplayName = builder.spDisplayName;
        codeGrantFlow = builder.codeGrantFlow;
        appObjectId = builder.appObjectId;
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
        private String deploymentAddress;

        private String spDisplayName;

        private Boolean codeGrantFlow;

        private String appObjectId;

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
