package com.sequenceiq.environment.api.v1.terms.model;

import jakarta.validation.constraints.NotNull;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public class TermsRequest {

    @NotNull(message = "Accepted value must be set")
    private Boolean accepted;

    @NotNull(message = "Term type must be set")
    private TermType termType;

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
        return "TermsRequest{" +
                "accepted=" + accepted +
                ", termType=" + termType +
                '}';
    }
}
