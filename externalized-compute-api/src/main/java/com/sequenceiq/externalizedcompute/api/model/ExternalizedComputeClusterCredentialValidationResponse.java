package com.sequenceiq.externalizedcompute.api.model;

import java.util.ArrayList;
import java.util.List;

import io.swagger.v3.oas.annotations.media.Schema;

public class ExternalizedComputeClusterCredentialValidationResponse {

    @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
    private boolean successful = true;

    private String message;

    @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
    private List<String> validationResults = new ArrayList<>();

    public ExternalizedComputeClusterCredentialValidationResponse() {
    }

    public ExternalizedComputeClusterCredentialValidationResponse(boolean successful, String message, List<String> validationResults) {
        this.successful = successful;
        this.message = message;
        this.validationResults = validationResults;
    }

    public boolean isSuccessful() {
        return successful;
    }

    public void setSuccessful(boolean successful) {
        this.successful = successful;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public List<String> getValidationResults() {
        return validationResults;
    }

    public void setValidationResults(List<String> validationResults) {
        this.validationResults = validationResults;
    }

    @Override
    public String toString() {
        return "ExternalizedComputeClusterCredentialValidationResponse{" +
                "successful=" + successful +
                ", message='" + message + '\'' +
                ", validationResults=" + validationResults +
                '}';
    }
}
