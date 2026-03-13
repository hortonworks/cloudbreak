package com.sequenceiq.remoteenvironment.api.v1.environment.model;

import java.util.Objects;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ValidateForDatalakeValidationResponse {

    private String validationType;

    private boolean passed;

    private String message;

    public String getValidationType() {
        return validationType;
    }

    public void setValidationType(String validationType) {
        this.validationType = validationType;
    }

    public boolean isPassed() {
        return passed;
    }

    public void setPassed(boolean passed) {
        this.passed = passed;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof ValidateForDatalakeValidationResponse that)) {
            return false;
        }
        return Objects.equals(validationType, that.validationType)
                && passed == that.passed
                && Objects.equals(message, that.message);
    }

    @Override
    public int hashCode() {
        return Objects.hash(validationType, passed, message);
    }

    @Override
    public String toString() {
        return "ValidateForDatalakeValidationResponse{" +
                "validationType='" + validationType + '\'' +
                ", valid=" + passed +
                ", message='" + message + '\'' +
                '}';
    }
}
