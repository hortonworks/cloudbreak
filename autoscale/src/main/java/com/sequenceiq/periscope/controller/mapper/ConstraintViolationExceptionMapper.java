package com.sequenceiq.periscope.controller.mapper;

import javax.validation.ConstraintViolation;
import javax.validation.ConstraintViolationException;
import javax.ws.rs.core.Response.Status;
import javax.ws.rs.ext.Provider;

import com.sequenceiq.cloudbreak.json.ValidationResult;

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

}
