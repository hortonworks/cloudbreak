package com.sequenceiq.environment.api.v1.environment.model.request;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(name = "FinishSetupCrossRealmTrustV1Request")
@JsonIgnoreProperties(ignoreUnknown = true)
public class FinishSetupCrossRealmTrustRequest {

    @Override
    public String toString() {
        return "FinishSetupCrossRealmTrustRequest{}";
    }
}
