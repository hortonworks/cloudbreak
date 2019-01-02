package com.sequenceiq.cloudbreak.api.endpoint.v4.credentials.responses;

import static com.sequenceiq.cloudbreak.doc.ModelDescriptions.EnvironmentRequestModelDescription.INTERACTIVE_LOGIN_CREDENTIAL_USER_CODE;
import static com.sequenceiq.cloudbreak.doc.ModelDescriptions.EnvironmentRequestModelDescription.INTERACTIVE_LOGIN_CREDENTIAL_VERIFICATION_URL;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

@ApiModel(value = "InteractiveCredential", description = "Contains values from an Azure interactive login attempt")
public class InteractiveCredentialV4Response {

    @ApiModelProperty(value = INTERACTIVE_LOGIN_CREDENTIAL_USER_CODE, example = "B8ZT2QD4K")
    private String userCode;

    @ApiModelProperty(value = INTERACTIVE_LOGIN_CREDENTIAL_VERIFICATION_URL, example = "https://microsoft.com/devicelogin")
    private String verificationUrl;

    public InteractiveCredentialV4Response() {
    }

    public InteractiveCredentialV4Response(String userCode, String verificationUrl) {
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

}
