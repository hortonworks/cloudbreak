package com.sequenceiq.freeipa.api.v1.freeipa.stack.model;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(name = "FreeIpaEnvironmentCrnResponse", description = "Parent environment CRN for a FreeIPA stack resource")
public class FreeIpaEnvironmentCrnResponse {

    @Schema(description = "Environment CRN that owns the FreeIPA stack")
    private String environmentCrn;

    public FreeIpaEnvironmentCrnResponse() {
    }

    public FreeIpaEnvironmentCrnResponse(String environmentCrn) {
        this.environmentCrn = environmentCrn;
    }

    public String getEnvironmentCrn() {
        return environmentCrn;
    }

    public void setEnvironmentCrn(String environmentCrn) {
        this.environmentCrn = environmentCrn;
    }
}
