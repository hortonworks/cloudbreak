package com.sequenceiq.environment.api.v1.environment.model.request;

import com.sequenceiq.environment.api.doc.environment.EnvironmentModelDescription;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

@ApiModel(value = "EditRazConfigurationV1Request")
public class EditRazConfigurationRequest {

    @ApiModelProperty(EnvironmentModelDescription.RAZ_SECURITY_GROUP)
    private String securityGroupIdForRaz;

    public void setSecurityGroupIdForRaz(String securityGroupIdForRaz) {
        this.securityGroupIdForRaz = securityGroupIdForRaz;
    }

    public String getSecurityGroupIdForRaz() {
        return securityGroupIdForRaz;
    }
}
