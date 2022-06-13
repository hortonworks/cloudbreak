package com.sequenceiq.cloudbreak.cloud.yarn.client.model.response;

import com.fasterxml.jackson.annotation.JsonProperty;

public class ApplicationErrorResponse  implements ApplicationResponse {

    private String code;

    private String errorMessage;

    private String diagnostics;

    @JsonProperty("error_message")
    public String getErrorMessage() {
        return errorMessage;
    }

    public void setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public String getDiagnostics() {
        return diagnostics;
    }

    public void setDiagnostics(String diagnostics) {
        this.diagnostics = diagnostics;
    }

    @Override
    public String toString() {
        return "ApplicationErrorResponse{" +
                "code='" + code + '\'' +
                ", errorMessage='" + errorMessage + '\'' +
                ", diagnostics='" + diagnostics + '\'' +
                '}';
    }
}
