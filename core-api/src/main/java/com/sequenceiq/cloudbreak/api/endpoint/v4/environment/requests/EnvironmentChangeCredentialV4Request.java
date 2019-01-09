package com.sequenceiq.cloudbreak.api.endpoint.v4.environment.requests;

import com.sequenceiq.cloudbreak.api.endpoint.v4.credentials.requests.CredentialV4Request;
import com.sequenceiq.cloudbreak.doc.ModelDescriptions.EnvironmentRequestModelDescription;

import io.swagger.annotations.ApiModelProperty;

public class EnvironmentChangeCredentialV4Request implements CredentialAwareEnvV4Request {

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
