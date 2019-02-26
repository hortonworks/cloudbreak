package com.sequenceiq.cloudbreak.json;

import java.util.HashMap;
import java.util.Map;

public class ValidationResult {

    static final String MESSAGE_SEPARATOR = "; ";

    private Map<String, String> validationErrors = new HashMap<>();

    public Map<String, String> getValidationErrors() {
        return validationErrors;
    }

    public void setValidationErrors(Map<String, String> validationErrors) {
        this.validationErrors.putAll(validationErrors);
    }

    public void setValidationError(String field, String message) {
        validationErrors.put(field, message);
    }

    public void addValidationError(String field, String message) {
        if (validationErrors.containsKey(field)) {
            message = validationErrors.get(field) + MESSAGE_SEPARATOR + message;
        }
        setValidationError(field, message);
    }

}
