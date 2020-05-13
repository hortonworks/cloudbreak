package com.sequenceiq.environment.api.v1.environment.model.response;

import com.sequenceiq.environment.api.doc.environment.EnvironmentModelDescription;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

@ApiModel(value = "RazConfigurationV1Response")
public class RazConfigurationResponse {

    @ApiModelProperty(EnvironmentModelDescription.RAZ_ENABLED)
    private boolean razEnabled;

    @ApiModelProperty(EnvironmentModelDescription.RAZ_SECURITY_GROUP)
    private String securityGroupIdForRaz;

    public boolean isRazEnabled() {
        return razEnabled;
    }

    public void setRazEnabled(boolean razEnabled) {
        this.razEnabled = razEnabled;
    }

    public String getSecurityGroupIdForRaz() {
        return securityGroupIdForRaz;
    }

    public void setSecurityGroupIdForRaz(String securityGroupIdForRaz) {
        this.securityGroupIdForRaz = securityGroupIdForRaz;
    }

    public static final class RazConfigurationResponseBuilder {
        private boolean razEnabled;

        private String securityGroupId;

        private RazConfigurationResponseBuilder() {
        }

        public static RazConfigurationResponseBuilder aRazResponse() {
            return new RazConfigurationResponseBuilder();
        }

        public RazConfigurationResponseBuilder withRazEnabled(boolean razEnabled) {
            this.razEnabled =  razEnabled;
            return this;
        }

        public RazConfigurationResponseBuilder withSecurity(String security) {
            this.securityGroupId =  security;
            return this;
        }

        public RazConfigurationResponse build() {
            RazConfigurationResponse razConfigurationResponse =  new RazConfigurationResponse();
            razConfigurationResponse.setRazEnabled(razEnabled);
            razConfigurationResponse.setSecurityGroupIdForRaz(securityGroupId);
            return razConfigurationResponse;
        }
    }
}
