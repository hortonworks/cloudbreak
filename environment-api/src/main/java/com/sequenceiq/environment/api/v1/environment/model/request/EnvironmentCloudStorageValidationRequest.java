package com.sequenceiq.environment.api.v1.environment.model.request;

import java.io.Serializable;

import javax.validation.constraints.NotNull;

import com.sequenceiq.common.api.backup.request.BackupRequest;
import com.sequenceiq.common.api.telemetry.request.TelemetryRequest;
import com.sequenceiq.environment.api.doc.environment.EnvironmentModelDescription;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

@ApiModel(value = "EnvironmentCloudStorageValidationRequest")
public class EnvironmentCloudStorageValidationRequest implements Serializable {

    @ApiModelProperty(EnvironmentModelDescription.CREDENTIAL_CRN)
    @NotNull
    private String credentialCrn;

    @ApiModelProperty(EnvironmentModelDescription.TELEMETRY)
    @NotNull
    private TelemetryRequest telemetry;

    @ApiModelProperty(EnvironmentModelDescription.TELEMETRY)
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
