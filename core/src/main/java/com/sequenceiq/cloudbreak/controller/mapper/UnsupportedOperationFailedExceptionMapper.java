package com.sequenceiq.cloudbreak.controller.mapper;

import javax.ws.rs.core.Response.Status;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.exception.mapper.BaseExceptionMapper;

@Component
public class UnsupportedOperationFailedExceptionMapper extends BaseExceptionMapper<UnsupportedOperationException> {

    @Override
    public Status getResponseStatus(UnsupportedOperationException exception) {
        return Status.BAD_REQUEST;
    }

    @Override
    public Class<UnsupportedOperationException> getExceptionType() {
        return UnsupportedOperationException.class;
    }
}