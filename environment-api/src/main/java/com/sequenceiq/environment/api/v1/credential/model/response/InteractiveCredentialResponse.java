package com.sequenceiq.environment.api.v1.credential.model.response;



import static com.sequenceiq.environment.api.doc.environment.EnvironmentModelDescription.INTERACTIVE_LOGIN_CREDENTIAL_USER_CODE;
import static com.sequenceiq.environment.api.doc.environment.EnvironmentModelDescription.INTERACTIVE_LOGIN_CREDENTIAL_VERIFICATION_URL;

import java.io.Serializable;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

@ApiModel(value = "InteractiveV1Credential", description = "Contains values from an Azure interactive login attempt")
public class InteractiveCredentialResponse implements Serializable {

    @ApiModelProperty(value = INTERACTIVE_LOGIN_CREDENTIAL_USER_CODE, example = "B8ZT2QD4K")
    private String userCode;

    @ApiModelProperty(value = INTERACTIVE_LOGIN_CREDENTIAL_VERIFICATION_URL, example = "https://microsoft.com/devicelogin")
    private String verificationUrl;

    public InteractiveCredentialResponse() {
    }

    public InteractiveCredentialResponse(String userCode, String verificationUrl) {
        this.userCode = userCode;
        this.verificationUrl = verificationUrl;
    }

    public String getUserCode() {
        return userCode;
    }

    public void setUserCode(String userCode) {
        this.userCode = userCode;
    }

    public String getVerificationUrl() {
        return verificationUrl;
    }

    public void setVerificationUrl(String verificationUrl) {
        this.verificationUrl = verificationUrl;
    }

    @Override
    public String toString() {
        return "InteractiveCredentialResponse{" +
                ", verificationUrl='" + verificationUrl + '\'' +
                '}';
    }
}
