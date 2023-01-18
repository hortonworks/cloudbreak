package com.sequenceiq.redbeams.api.endpoint.v4.databaseserver.requests;

import javax.validation.constraints.NotNull;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.sequenceiq.cloudbreak.auth.crn.CrnResourceDescriptor;
import com.sequenceiq.cloudbreak.rotation.annotation.ValidSecretType;
import com.sequenceiq.cloudbreak.rotation.request.BaseSecretRotationRequest;
import com.sequenceiq.cloudbreak.validation.ValidCrn;
import com.sequenceiq.redbeams.doc.ModelDescriptions;
import com.sequenceiq.redbeams.rotation.RedbeamsSecretType;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = ModelDescriptions.ROTATE_DATABASE_SERVER_SECRETS_REQUEST)
@JsonIgnoreProperties(ignoreUnknown = true)
public class RotateDatabaseServerSecretV4Request extends BaseSecretRotationRequest {

    @ValidCrn(resource = { CrnResourceDescriptor.DATABASE_SERVER })
    private String crn;

    @ValidSecretType(allowedTypes = { RedbeamsSecretType.class })
    @NotNull
    private String secret;

    public String getCrn() {
        return crn;
    }

    public void setCrn(String crn) {
        this.crn = crn;
    }

    public String getSecret() {
        return secret;
    }

    public void setSecret(String secret) {
        this.secret = secret;
    }
}
