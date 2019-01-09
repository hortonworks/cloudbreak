package com.sequenceiq.cloudbreak.api.endpoint.v4.environment.requests;

import com.sequenceiq.cloudbreak.api.model.CredentialRequest;
import com.sequenceiq.cloudbreak.doc.ModelDescriptions.EnvironmentRequestModelDescription;

import io.swagger.annotations.ApiModelProperty;

public class EnvironmentChangeCredentialV4Request implements CredentialAwareEnvV4Request {

    @ApiModelProperty(EnvironmentRequestModelDescription.CREDENTIAL_NAME)
    private String credentialName;

    @ApiModelProperty(EnvironmentRequestModelDescription.CREDENTIAL)
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
