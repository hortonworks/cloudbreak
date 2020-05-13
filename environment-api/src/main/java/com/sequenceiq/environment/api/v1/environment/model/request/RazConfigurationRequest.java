package com.sequenceiq.environment.api.v1.environment.model.request;

import java.io.Serializable;

import com.sequenceiq.environment.api.doc.environment.EnvironmentModelDescription;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

@ApiModel(value = "RazConfigurationV1Request")
public class RazConfigurationRequest implements Serializable {

    @ApiModelProperty(EnvironmentModelDescription.RAZ_ENABLED)
    private Boolean razEnabled;

    @ApiModelProperty(EnvironmentModelDescription.RAZ_SECURITY_GROUP)
    private String securityGroupIdForRaz;

    public Boolean getRazEnabled() {
        return razEnabled;
    }

    public void setRazEnabled(Boolean razEnabled) {
        this.razEnabled = razEnabled;
    }

    public String getSecurityGroupIdForRaz() {
        return securityGroupIdForRaz;
    }

    public void setSecurityGroupIdForRaz(String securityGroupIdForRaz) {
        this.securityGroupIdForRaz = securityGroupIdForRaz;
    }
}
