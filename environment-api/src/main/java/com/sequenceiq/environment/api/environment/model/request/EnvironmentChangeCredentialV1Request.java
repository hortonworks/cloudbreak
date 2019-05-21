package com.sequenceiq.environment.api.environment.model.request;

import com.sequenceiq.environment.api.v1.credential.model.request.CredentialRequest;
import com.sequenceiq.environment.api.environment.doc.EnvironmentModelDescription;

import io.swagger.annotations.ApiModelProperty;

public class EnvironmentChangeCredentialV1Request implements CredentialAwareEnvV1Request {

    @ApiModelProperty(EnvironmentModelDescription.CREDENTIAL_NAME_REQUEST)
    private String credentialName;

    @ApiModelProperty(EnvironmentModelDescription.CREDENTIAL_REQUEST)
    private CredentialRequest credential;

    @Override
    public String getCredentialName() {
        return credentialName;
    }

    @Override
    public void setCredentialName(String credentialName) {
        this.credentialName = credentialName;
    }

    @Override
    public CredentialRequest getCredential() {
        return credential;
    }

    @Override
    public void setCredential(CredentialRequest credential) {
        this.credential = credential;
    }
}
