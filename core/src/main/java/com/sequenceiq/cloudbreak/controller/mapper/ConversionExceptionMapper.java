package com.sequenceiq.cloudbreak.controller.mapper;

import javax.ws.rs.core.Response.Status;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.converter.ConversionException;

@Component
public class ConversionExceptionMapper extends SendNotificationExceptionMapper<ConversionException> {

    @Override
    public Status getResponseStatus(ConversionException exception) {
        return Status.BAD_REQUEST;
    }

    @Override
    public Class<ConversionException> getExceptionType() {
        return ConversionException.class;
    }

}