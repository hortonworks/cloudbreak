package com.sequenceiq.sdx.api.model;

import java.util.StringJoiner;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class SdxRecoveryRequest {

    @Schema(description = ModelDescriptions.RECOVERY_FORCE)
    private boolean force;

    public boolean isForced() {
        return force;
    }

    public void setForceFlag(boolean force) {
        this.force = force;
    }

    @Override
    public String toString() {
        return new StringJoiner(", ", SdxRecoveryRequest.class.getSimpleName() + "[", "]")
                .add("force=" + force)
                .toString();
    }
}
