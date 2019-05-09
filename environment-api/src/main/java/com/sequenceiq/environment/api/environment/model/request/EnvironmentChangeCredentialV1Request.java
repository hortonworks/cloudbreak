package com.sequenceiq.environment.api.environment.model.request;

import com.sequenceiq.environment.api.credential.model.request.CredentialV1Request;
import com.sequenceiq.environment.api.environment.doc.EnvironmentModelDescription;

import io.swagger.annotations.ApiModelProperty;

public class EnvironmentChangeCredentialV1Request implements CredentialAwareEnvV1Request {

    @ApiModelProperty(EnvironmentModelDescription.CREDENTIAL_NAME_REQUEST)
    private String credentialName;

    @ApiModelProperty(EnvironmentModelDescription.CREDENTIAL_REQUEST)
    private CredentialV1Request credential;

    @Override
    public String getCredentialName() {
        return credentialName;
    }

    @Override
    public void setCredentialName(String credentialName) {
        this.credentialName = credentialName;
    }

    @Override
    public CredentialV1Request getCredential() {
        return credential;
    }

    @Override
    public void setCredential(CredentialV1Request credential) {
        this.credential = credential;
    }
}
