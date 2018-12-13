package com.sequenceiq.cloudbreak.controller.mapper;

import java.util.ArrayList;
import java.util.List;

import javax.validation.ConstraintViolation;
import javax.validation.ConstraintViolationException;
import javax.ws.rs.core.Response.Status;

import org.springframework.stereotype.Component;

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
        List<String> result = new ArrayList<>();
        for (ConstraintViolation<?> violation : exception.getConstraintViolations()) {
            result.add(violation.getPropertyPath() + ": " + violation.getInvalidValue() + ", error: " + violation.getMessage());
        }
        return String.join("\n", result);
    }

    @Override
    Status getResponseStatus() {
        return Status.BAD_REQUEST;
    }

    @Override
    Class<ConstraintViolationException> getExceptionType() {
        return ConstraintViolationException.class;
    }

}
