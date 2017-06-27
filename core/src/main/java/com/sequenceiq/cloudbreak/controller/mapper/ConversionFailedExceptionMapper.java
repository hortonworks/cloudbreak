package com.sequenceiq.cloudbreak.controller.mapper;

import javax.ws.rs.core.Response;
import javax.ws.rs.ext.Provider;

import org.springframework.core.convert.ConversionFailedException;

@Provider
public class ConversionFailedExceptionMapper extends SendNotificationExceptionMapper<ConversionFailedException> {

    @Override
    Response.Status getResponseStatus() {
        return Response.Status.BAD_REQUEST;
    }
}