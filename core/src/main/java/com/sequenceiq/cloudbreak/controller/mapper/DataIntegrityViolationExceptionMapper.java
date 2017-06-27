package com.sequenceiq.cloudbreak.controller.mapper;

import javax.ws.rs.core.Response;
import javax.ws.rs.ext.Provider;

import org.springframework.dao.DataIntegrityViolationException;

@Provider
public class DataIntegrityViolationExceptionMapper extends SendNotificationExceptionMapper<DataIntegrityViolationException> {

    @Override
    protected Object getEntity(DataIntegrityViolationException exception) {
        return exception.getLocalizedMessage();
    }

    @Override
    Response.Status getResponseStatus() {
        return Response.Status.BAD_REQUEST;
    }
}
