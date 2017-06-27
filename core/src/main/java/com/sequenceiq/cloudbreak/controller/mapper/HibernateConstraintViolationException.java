package com.sequenceiq.cloudbreak.controller.mapper;

import javax.ws.rs.core.Response;
import javax.ws.rs.ext.Provider;

import org.hibernate.exception.ConstraintViolationException;

@Provider
public class HibernateConstraintViolationException extends SendNotificationExceptionMapper<ConstraintViolationException> {

    @Override
    protected Object getEntity(ConstraintViolationException exception) {
        return exception.getLocalizedMessage();
    }

    @Override
    Response.Status getResponseStatus() {
        return Response.Status.BAD_REQUEST;
    }
}
