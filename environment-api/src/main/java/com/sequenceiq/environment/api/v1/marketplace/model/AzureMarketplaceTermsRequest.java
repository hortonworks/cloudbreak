package com.sequenceiq.environment.api.v1.marketplace.model;

import javax.validation.constraints.NotNull;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public class AzureMarketplaceTermsRequest {

    @NotNull
    private Boolean accepted;

    public Boolean getAccepted() {
        return accepted;
    }

    public void setAccepted(Boolean accepted) {
        this.accepted = accepted;
    }

    @Override
    public String toString() {
        return "AzureMarketplaceTermsRequest{" +
                "accepted=" + accepted +
                '}';
    }
}
