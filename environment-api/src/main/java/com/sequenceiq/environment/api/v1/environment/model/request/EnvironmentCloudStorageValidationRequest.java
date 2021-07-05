package com.sequenceiq.environment.api.v1.environment.model.request;

import java.io.Serializable;

import javax.validation.constraints.NotNull;

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

    @Override
    public String toString() {
        return "EnvironmentCloudStorageValidationRequest{" +
                "credentialCrn='" + credentialCrn + '\'' +
                ", telemetry=" + telemetry +
                '}';
    }
}
