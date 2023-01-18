package com.sequenceiq.environment.api.v1.environment.model.request;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.sequenceiq.environment.api.doc.environment.EnvironmentModelDescription;
import com.sequenceiq.environment.api.v1.credential.model.request.CredentialRequest;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(name = "EnvironmentChangeCredentialV1Request")
@JsonIgnoreProperties(ignoreUnknown = true)
public class EnvironmentChangeCredentialRequest implements CredentialAwareEnvRequest {

    @Schema(description = EnvironmentModelDescription.CREDENTIAL_NAME_REQUEST)
    private String credentialName;

    @Schema(description = EnvironmentModelDescription.CREDENTIAL_REQUEST)
    private CredentialRequest credential;

    @Override
    public String getCredentialName() {
        return credentialName;
    }

    @Override
    public void setCredentialName(String credentialName) {
        this.credentialName = credentialName;
    }

    public CredentialRequest getCredential() {
        return credential;
    }

    public void setCredential(CredentialRequest credential) {
        this.credential = credential;
    }

    @Override
    public String toString() {
        return "EnvironmentChangeCredentialRequest{" +
                "credentialName='" + credentialName + '\'' +
                ", credential=" + credential +
                '}';
    }
}
