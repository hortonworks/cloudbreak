package com.sequenceiq.periscope.controller.mapper;

import javax.validation.ConstraintViolation;
import javax.validation.ConstraintViolationException;
import javax.ws.rs.core.Response.Status;
import javax.ws.rs.ext.Provider;

import com.sequenceiq.periscope.api.model.ExceptionResult;

@Provider
public class ConstraintViolationExceptionMapper extends BaseExceptionMapper<ConstraintViolationException> {

    @Override
    protected Object getEntity(ConstraintViolationException exception) {
        for (ConstraintViolation<?> violation : exception.getConstraintViolations()) {
            return new ExceptionResult(violation.getMessage());
        }
        return new ExceptionResult(exception.getMessage());
    }

    @Override
    Status getResponseStatus() {
        return Status.BAD_REQUEST;
    }
}
