package com.sequenceiq.environment.api.v1.marketplace.model;

import jakarta.validation.constraints.NotNull;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import io.swagger.v3.oas.annotations.media.Schema;

@JsonIgnoreProperties(ignoreUnknown = true)
public class AzureMarketplaceTermsResponse {

    @NotNull
    @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
    private Boolean accepted = Boolean.FALSE;

    public AzureMarketplaceTermsResponse() {
    }

    public AzureMarketplaceTermsResponse(Boolean accepted) {
        this.accepted = accepted;
    }

    public Boolean getAccepted() {
        return accepted;
    }

    public void setAccepted(Boolean accepted) {
        this.accepted = accepted;
    }

    @Override
    public String toString() {
        return "AzureMarketplaceTermsResponse{" +
                "accepted=" + accepted +
                '}';
    }
}
