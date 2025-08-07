package com.sequenceiq.environment.api.v1.terms.model;

import jakarta.validation.constraints.NotNull;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import io.swagger.v3.oas.annotations.media.Schema;

@JsonIgnoreProperties(ignoreUnknown = true)
public class TermsResponse {

    @NotNull
    @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
    private Boolean accepted = Boolean.FALSE;

    private TermType termType;

    public TermsResponse() {
    }

    public TermsResponse(Boolean accepted, TermType termType) {
        this.accepted = accepted;
        this.termType = termType;
    }

    public Boolean getAccepted() {
        return accepted;
    }

    public void setAccepted(Boolean accepted) {
        this.accepted = accepted;
    }

    public TermType getTermType() {
        return termType;
    }

    public void setTermType(TermType termType) {
        this.termType = termType;
    }

    @Override
    public String toString() {
        return "TermsResponse{" +
                "accepted=" + accepted +
                ", termType=" + termType +
                '}';
    }
}
