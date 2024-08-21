package com.sequenceiq.externalizedcompute.api.model;

import java.util.List;

public class ExternalizedComputeClusterCredentialValidationResponse {

    private boolean successful = true;

    private String message;

    private List<String> validationResults;

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
