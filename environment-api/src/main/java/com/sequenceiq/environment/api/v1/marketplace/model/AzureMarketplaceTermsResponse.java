package com.sequenceiq.environment.api.v1.marketplace.model;

import javax.validation.constraints.NotNull;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public class AzureMarketplaceTermsResponse {

    @NotNull
    Boolean accepted;

    public AzureMarketplaceTermsResponse(Boolean accepted) {
        this.accepted = accepted;
    }

    public Boolean getAccepted() {
        return accepted;
    }

    public void setAccepted(Boolean accepted) {
        this.accepted = accepted;
    }
}
