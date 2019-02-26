package com.sequenceiq.cloudbreak.controller.mapper;

import javax.validation.ConstraintViolation;
import javax.validation.ConstraintViolationException;
import javax.ws.rs.core.Response.Status;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.json.ValidationResult;

/**
 * An exception mapper for constraint violation exceptions.
 */
@Component
public class ConstraintViolationExceptionMapper extends SendNotificationExceptionMapper<ConstraintViolationException> {

    @Override
    protected String getErrorMessage(ConstraintViolationException exception) {
        StringBuilder violations = new StringBuilder();
        for (ConstraintViolation<?> constraintViolation : exception.getConstraintViolations()) {
            violations.append("constraintviolation: ")
                    .append(constraintViolation.getPropertyPath())
                    .append(" - ")
                    .append(constraintViolation.getMessage())
                    .append('\n');
        }
        return violations.toString();
    }

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

    @Override
    Class<ConstraintViolationException> getExceptionType() {
        return ConstraintViolationException.class;
    }

    private void addValidationError(ConstraintViolation<?> violation, ValidationResult validationResult) {
        String propertyPath = violation.getPropertyPath() != null ? violation.getPropertyPath().toString() : "";
        validationResult.addValidationError(propertyPath, violation.getMessage());
    }

}
