package com.sequenceiq.environment.api.v1.credential.model.response;



import static com.sequenceiq.environment.api.doc.environment.EnvironmentModelDescription.ARM_TEMPLATE;
import static com.sequenceiq.environment.api.doc.environment.EnvironmentModelDescription.INTERACTIVE_LOGIN_CREDENTIAL_USER_CODE;
import static com.sequenceiq.environment.api.doc.environment.EnvironmentModelDescription.INTERACTIVE_LOGIN_CREDENTIAL_VERIFICATION_URL;
import static com.sequenceiq.environment.api.doc.environment.EnvironmentModelDescription.SERVICE_PRINCIPAL_ID;

import java.io.Serializable;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

@ApiModel(value = "LightHouseCredentialResponse", description = "Contains values from an Azure LightHouse response")
public class LightHouseCredentialResponse implements Serializable {

    @ApiModelProperty(value = ARM_TEMPLATE, example = "{}")
    private String armTemplate;

    @ApiModelProperty(value = SERVICE_PRINCIPAL_ID, example = "1234-abcd-ergt-6789")
    private String servicePrincipalId;

    public LightHouseCredentialResponse() {
    }

    public LightHouseCredentialResponse(String armTemplate, String servicePrincipalId) {
        this.armTemplate = armTemplate;
        this.servicePrincipalId = servicePrincipalId;
    }

    public String getArmTemplate() {
        return armTemplate;
    }

    public void setArmTemplate(String armTemplate) {
        this.armTemplate = armTemplate;
    }

    public String getServicePrincipalId() {
        return servicePrincipalId;
    }

    public void setServicePrincipalId(String servicePrincipalId) {
        this.servicePrincipalId = servicePrincipalId;
    }

    @Override
    public String toString() {
        return "LightHouseCredentialResponse{" +
                "armTemplate='" + armTemplate + '\'' +
                ", servicePrincipalId='" + servicePrincipalId + '\'' +
                '}';
    }
}
