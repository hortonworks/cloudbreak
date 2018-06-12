package com.sequenceiq.cloudbreak.controller.mapper;

import javax.ws.rs.core.Response.Status;

import org.springframework.stereotype.Component;

@Component
public class UnsupportedOperationFailedExceptionMapper extends BaseExceptionMapper<UnsupportedOperationException> {

    @Override
    Status getResponseStatus() {
        return Status.BAD_REQUEST;
    }

    @Override
    public Class<UnsupportedOperationException> supportedType() {
        return UnsupportedOperationException.class;
    }
}