package com.sequenceiq.cloudbreak.controller.mapper;

import javax.ws.rs.core.Response.Status;

import org.springframework.stereotype.Component;

@Component
public class UnsupportedOperationFailedExceptionMapper extends BaseExceptionMapper<UnsupportedOperationException> {

    @Override
    Status getResponseStatus(UnsupportedOperationException exception) {
        return Status.BAD_REQUEST;
    }

    @Override
    Class<UnsupportedOperationException> getExceptionType() {
        return UnsupportedOperationException.class;
    }
}