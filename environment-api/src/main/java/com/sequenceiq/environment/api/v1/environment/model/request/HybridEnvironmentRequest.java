package com.sequenceiq.environment.api.v1.environment.model.request;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.sequenceiq.environment.api.doc.environment.EnvironmentModelDescription;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(name = "HybridEnvironmentRequest")
@JsonIgnoreProperties(ignoreUnknown = true)
public class HybridEnvironmentRequest {

    @Schema(description = EnvironmentModelDescription.REMOTE_ENVIRONMENT_CRN)
    private String remoteEnvironmentCrn;

    public String getRemoteEnvironmentCrn() {
        return remoteEnvironmentCrn;
    }

    public void setRemoteEnvironmentCrn(String remoteEnvironmentCrn) {
        this.remoteEnvironmentCrn = remoteEnvironmentCrn;
    }

    @Override
    public String toString() {
        return "HybridEnvironmentRequest{" +
                "remoteEnvironmentCrn='" + remoteEnvironmentCrn + '\'' +
                '}';
    }
}
