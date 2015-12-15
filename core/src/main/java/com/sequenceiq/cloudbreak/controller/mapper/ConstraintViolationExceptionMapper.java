package com.sequenceiq.cloudbreak.controller.mapper;

import javax.validation.ConstraintViolation;
import javax.validation.ConstraintViolationException;
import javax.ws.rs.core.Response;
import javax.ws.rs.ext.ExceptionMapper;
import javax.ws.rs.ext.Provider;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sequenceiq.cloudbreak.controller.json.ValidationResult;
import com.sequenceiq.cloudbreak.logger.MDCBuilder;

@Provider
public class ConstraintViolationExceptionMapper implements ExceptionMapper<ConstraintViolationException> {

    private static final Logger LOGGER = LoggerFactory.getLogger(ConstraintViolationExceptionMapper.class);

    @Override
    public Response toResponse(ConstraintViolationException exception) {
        MDCBuilder.buildMdcContext();
        ValidationResult result = new ValidationResult();
        for (ConstraintViolation violation : exception.getConstraintViolations()) {
            String key = "";
            if (violation.getPropertyPath() != null) {
                key = violation.getPropertyPath().toString();
            }
            result.addValidationError(key, violation.getMessage());
        }
        LOGGER.error(exception.getMessage());
        return Response.status(Response.Status.BAD_REQUEST).entity(result)
                .build();
    }
}
