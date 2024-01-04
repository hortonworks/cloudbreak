package com.sequenceiq.periscope.controller.mapper;

import java.util.HashMap;
import java.util.Map;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import jakarta.ws.rs.core.Response.Status;
import jakarta.ws.rs.ext.Provider;

@Provider
public class ConstraintViolationExceptionMapper extends BaseExceptionMapper<ConstraintViolationException> {

    @Override
    protected Object getEntity(ConstraintViolationException exception) {
        ValidationResult validationResult = new ValidationResult();
        exception.getConstraintViolations()
                .forEach(violation -> addValidationError(violation, validationResult));
        return validationResult;
    }

    @Override
    Status getResponseStatus() {
        return Status.BAD_REQUEST;
    }

    private void addValidationError(ConstraintViolation<?> violation, ValidationResult validationResult) {
        String propertyPath = violation.getPropertyPath() != null ? violation.getPropertyPath().toString() : "";
        validationResult.addValidationError(propertyPath, violation.getMessage());
    }

    private static class ValidationResult {

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

}
