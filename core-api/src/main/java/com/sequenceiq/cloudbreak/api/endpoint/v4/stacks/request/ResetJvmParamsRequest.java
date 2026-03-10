package com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema
@JsonIgnoreProperties(ignoreUnknown = true)
public class ResetJvmParamsRequest {

    @Schema(description = "If true, only validates the operation without triggering the actual flow", defaultValue = "false")
    private boolean dryRun;

    public boolean isDryRun() {
        return dryRun;
    }

    public void setDryRun(boolean dryRun) {
        this.dryRun = dryRun;
    }
}
