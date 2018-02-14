package com.sequenceiq.periscope.controller.mapper;

import javax.ws.rs.core.Response.Status;
import javax.ws.rs.ext.Provider;

import org.springframework.dao.DataIntegrityViolationException;

import com.sequenceiq.periscope.api.model.ExceptionResult;

@Provider
public class DataIntegrityViolationExceptionMapper extends BaseExceptionMapper<DataIntegrityViolationException> {

    @Override
    protected Object getEntity(DataIntegrityViolationException exception) {
        return new ExceptionResult(exception.getLocalizedMessage());
    }

    @Override
    Status getResponseStatus() {
        return Status.BAD_REQUEST;
    }
}
