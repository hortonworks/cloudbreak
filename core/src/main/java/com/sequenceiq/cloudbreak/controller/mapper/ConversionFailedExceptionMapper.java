package com.sequenceiq.cloudbreak.controller.mapper;

import javax.ws.rs.core.Response.Status;

import org.springframework.core.convert.ConversionFailedException;
import org.springframework.stereotype.Component;

@Component
public class ConversionFailedExceptionMapper extends SendNotificationExceptionMapper<ConversionFailedException> {

    @Override
    public Status getResponseStatus(ConversionFailedException exception) {
        return Status.BAD_REQUEST;
    }

    @Override
    public Class<ConversionFailedException> getExceptionType() {
        return ConversionFailedException.class;
    }
}