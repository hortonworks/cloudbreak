package com.sequenceiq.environment.api.environment.model.requests;

import com.sequenceiq.environment.api.environment.doc.EnvironmentRequestModelDescription;

import io.swagger.annotations.ApiModelProperty;

public class EnvironmentChangeCredentialV4Request implements CredentialAwareEnvRequest {

    @ApiModelProperty(EnvironmentRequestModelDescription.CREDENTIAL_NAME)
    private String credentialName;

    @ApiModelProperty(EnvironmentRequestModelDescription.CREDENTIAL)
    private CredentialV4Request credential;

    @Override
    public String getCredentialName() {
        return credentialName;
    }

    @Override
    public void setCredentialName(String credentialName) {
        this.credentialName = credentialName;
    }

    @Override
    public CredentialV4Request getCredential() {
        return credential;
    }

    @Override
    public void setCredential(CredentialV4Request credential) {
        this.credential = credential;
    }
}
