package com.sequenceiq.cloudbreak.controller.mapper;

import org.hibernate.exception.ConstraintViolationException;

import javax.ws.rs.core.Response.Status;
import javax.ws.rs.ext.Provider;

@Provider
public class HibernateConstraintViolationExceptionMapper extends SendNotificationExceptionMapper<ConstraintViolationException> {

    @Override
    protected Object getEntity(ConstraintViolationException exception) {
        return exception.getLocalizedMessage();
    }

    @Override
    Status getResponseStatus() {
        return Status.BAD_REQUEST;
    }
}
