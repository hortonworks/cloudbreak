package com.sequenceiq.cloudbreak.controller.mapper;

import org.springframework.dao.DataIntegrityViolationException;

import javax.ws.rs.core.Response.Status;
import javax.ws.rs.ext.Provider;

@Provider
public class DataIntegrityViolationExceptionMapper extends SendNotificationExceptionMapper<DataIntegrityViolationException> {

    @Override
    protected Object getEntity(DataIntegrityViolationException exception) {
        return exception.getLocalizedMessage();
    }

    @Override
    Status getResponseStatus() {
        return Status.BAD_REQUEST;
    }
}
