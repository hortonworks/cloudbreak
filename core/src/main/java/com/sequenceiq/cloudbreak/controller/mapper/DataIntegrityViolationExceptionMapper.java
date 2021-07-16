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
    public Status getResponseStatus(DataIntegrityViolationException exception) {
        return Status.BAD_REQUEST;
    }

    @Override
    public Class<DataIntegrityViolationException> getExceptionType() {
        return DataIntegrityViolationException.class;
    }
}
