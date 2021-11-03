package com.sequenceiq.cloudbreak.exception.mapper;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import javax.validation.ConstraintViolation;
import javax.validation.ConstraintViolationException;
import javax.ws.rs.core.Response;

import org.springframework.stereotype.Component;

@Component
public class ConstraintViolationExceptionMapper extends BaseExceptionMapper<ConstraintViolationException> {

    @Override
    public Response.Status getResponseStatus(ConstraintViolationException exception) {
        return Response.Status.BAD_REQUEST;
    }

    @Override
    protected String getErrorMessage(ConstraintViolationException exception) {
        if (exception.getConstraintViolations().size() == 1) {
            return exception.getConstraintViolations().stream().findFirst().get().getMessage();
        }
        return "More than one validation errors happened: \n" +
                exception.getConstraintViolations()
                        .stream()
                        .sorted((o1, o2) -> o1.getMessage().equals(o2.getMessage()) ? 0 : 1)
                        .map(ConstraintViolation::getMessage).collect(Collectors.joining("\n"));
    }

    @Override
    public Class<ConstraintViolationException> getExceptionType() {
        return ConstraintViolationException.class;
    }

    @Override
    protected Object getPayload(ConstraintViolationException exception) {
        List<ValidationResultResponse> validationResults = new ArrayList<>();
        exception.getConstraintViolations().stream()
                .sorted((o1, o2) -> o1.equals(o2) ? 0 : 1)
                .forEach(violation -> {
                    String propertyPath = violation.getPropertyPath() != null ? violation.getPropertyPath().toString() : "";
                    validationResults.add(new ValidationResultResponse(propertyPath, violation.getMessage()));
                });
        return validationResults;
    }
}