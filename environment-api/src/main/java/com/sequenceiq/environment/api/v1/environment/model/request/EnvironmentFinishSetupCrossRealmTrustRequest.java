package com.sequenceiq.environment.api.v1.environment.model.request;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(name = "EnvironmentFinishSetupV1CrossRealmTrustRequest")
@JsonIgnoreProperties(ignoreUnknown = true)
public class EnvironmentFinishSetupCrossRealmTrustRequest {

    @Override
    public String toString() {
        return "EnvironmentPrepareCrossRealmTrustRequest{}";
    }
}
