package com.sequenceiq.provisioning.controller.json;

import java.util.HashMap;
import java.util.Map;

public class ValidationResult {

    private Map<String, String> validationErrors = new HashMap<>();

    public Map<String, String> getValidationErrors() {
        return validationErrors;
    }

    public void setValidationErrors(Map<String, String> validationErrors) {
        this.validationErrors = validationErrors;
    }

    public void addValidationError(String field, String message) {
        validationErrors.put(field, message);
    }

}
