package com.sequenceiq.redbeams.api.endpoint.v4.databaseserver.requests;

import jakarta.validation.constraints.NotEmpty;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.sequenceiq.cloudbreak.auth.crn.CrnResourceDescriptor;
import com.sequenceiq.cloudbreak.auth.security.internal.ResourceCrn;
import com.sequenceiq.cloudbreak.rotation.annotation.ValidSecretType;
import com.sequenceiq.cloudbreak.validation.ValidCrn;
import com.sequenceiq.redbeams.doc.ModelDescriptions;
import com.sequenceiq.redbeams.rotation.RedbeamsSecretType;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = ModelDescriptions.CLEANUP_SECRET_ROTATION_PROGRESS_REQUEST)
@JsonIgnoreProperties(ignoreUnknown = true)
public class SecretRotationCleanupProgressV4Request {

    @ResourceCrn
    @ValidCrn(resource = { CrnResourceDescriptor.DATABASE_SERVER })
    private String crn;

    @ValidSecretType(allowedTypes = { RedbeamsSecretType.class })
    @NotEmpty
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

    @Override
    public String toString() {
        return "SecretRotationCleanupProgressV4Request{" +
                "crn='" + crn + '\'' +
                ", secret='" + secret + '\'' +
                '}';
    }
}
