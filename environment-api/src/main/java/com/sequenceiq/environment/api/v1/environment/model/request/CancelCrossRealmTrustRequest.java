package com.sequenceiq.environment.api.v1.environment.model.request;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(name = "CancelCrossRealmTrustV1Request")
@JsonIgnoreProperties(ignoreUnknown = true)
public class CancelCrossRealmTrustRequest {
    @Override
    public String toString() {
        return "CancelCrossRealmTrustRequest{}";
    }
}
