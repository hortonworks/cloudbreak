package com.sequenceiq.redbeams.controller.mapper;

import javax.ws.rs.core.Response.Status;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.exception.mapper.BaseExceptionMapper;

@Component
public class IllegalStateExceptionMapper extends BaseExceptionMapper<IllegalStateException> {

    @Override
    public Status getResponseStatus(IllegalStateException exception) {
        return Status.BAD_REQUEST;
    }

    @Override
    public Class<IllegalStateException> getExceptionType() {
        return IllegalStateException.class;
    }
}
