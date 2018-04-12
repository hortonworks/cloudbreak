package com.sequenceiq.cloudbreak.controller.mapper;

import org.springframework.core.convert.ConversionFailedException;

import javax.ws.rs.core.Response.Status;
import javax.ws.rs.ext.Provider;

@Provider
public class ConversionFailedExceptionMapper extends SendNotificationExceptionMapper<ConversionFailedException> {

    @Override
    Status getResponseStatus() {
        return Status.BAD_REQUEST;
    }
}