package com.sequenceiq.environment.api.v1.environment.model.request;

import java.io.Serializable;

import jakarta.validation.constraints.NotNull;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.sequenceiq.common.api.backup.request.BackupRequest;
import com.sequenceiq.common.api.telemetry.request.TelemetryRequest;
import com.sequenceiq.environment.api.doc.environment.EnvironmentModelDescription;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(name = "EnvironmentCloudStorageValidationRequest")
@JsonIgnoreProperties(ignoreUnknown = true)
public class EnvironmentCloudStorageValidationRequest implements Serializable {

    @Schema(description = EnvironmentModelDescription.CREDENTIAL_CRN)
    @NotNull
    private String credentialCrn;

    @Schema(description = EnvironmentModelDescription.TELEMETRY)
    @NotNull
    private TelemetryRequest telemetry;

    @Schema(description = EnvironmentModelDescription.TELEMETRY)
    private BackupRequest backup;

    public String getCredentialCrn() {
        return credentialCrn;
    }

    public TelemetryRequest getTelemetry() {
        return telemetry;
    }

    public void setCredentialCrn(String credentialCrn) {
        this.credentialCrn = credentialCrn;
    }

    public void setTelemetry(TelemetryRequest telemetry) {
        this.telemetry = telemetry;
    }

    public BackupRequest getBackup() {
        return backup;
    }

    public void setBackup(BackupRequest backup) {
        this.backup = backup;
    }

    @Override
    public String toString() {
        return "EnvironmentCloudStorageValidationRequest{" +
                "credentialCrn='" + credentialCrn + '\'' +
                ", telemetry=" + telemetry +
                '}';
    }
}
