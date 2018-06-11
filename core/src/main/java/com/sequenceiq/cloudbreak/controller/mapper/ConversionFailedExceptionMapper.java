package com.sequenceiq.cloudbreak.controller.mapper;

import javax.ws.rs.core.Response.Status;

import org.springframework.core.convert.ConversionFailedException;
import org.springframework.stereotype.Component;

@Component
public class ConversionFailedExceptionMapper extends SendNotificationExceptionMapper<ConversionFailedException> {

    @Override
    Status getResponseStatus() {
        return Status.BAD_REQUEST;
    }

    @Override
    public Class<ConversionFailedException> supportedType() {
        return ConversionFailedException.class;
    }
}