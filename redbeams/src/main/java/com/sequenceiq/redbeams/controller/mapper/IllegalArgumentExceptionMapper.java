package com.sequenceiq.redbeams.controller.mapper;

import jakarta.ws.rs.core.Response.Status;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.exception.mapper.BaseExceptionMapper;

@Component
public class IllegalArgumentExceptionMapper extends BaseExceptionMapper<IllegalArgumentException> {

    @Override
    public Status getResponseStatus(IllegalArgumentException exception) {
        return Status.BAD_REQUEST;
    }

    @Override
    public Class<IllegalArgumentException> getExceptionType() {
        return IllegalArgumentException.class;
    }
}
