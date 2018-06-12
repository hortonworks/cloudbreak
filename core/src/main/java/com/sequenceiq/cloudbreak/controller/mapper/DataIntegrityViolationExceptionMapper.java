package com.sequenceiq.cloudbreak.controller.mapper;

import javax.ws.rs.core.Response.Status;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Component;

@Component
public class DataIntegrityViolationExceptionMapper extends SendNotificationExceptionMapper<DataIntegrityViolationException> {

    @Override
    protected Object getEntity(DataIntegrityViolationException exception) {
        return exception.getLocalizedMessage();
    }

    @Override
    Status getResponseStatus() {
        return Status.BAD_REQUEST;
    }

    @Override
    public Class<DataIntegrityViolationException> supportedType() {
        return DataIntegrityViolationException.class;
    }
}
