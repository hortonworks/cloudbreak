package com.sequenceiq.datalake.controller.mapper;

import javax.ws.rs.core.Response.Status;

import org.springframework.stereotype.Component;

@Component
public class IllegalStateExceptionMapper extends BaseExceptionMapper<IllegalStateException> {

    @Override
    Status getResponseStatus() {
        return Status.BAD_REQUEST;
    }

    @Override
    Class<IllegalStateException> getExceptionType() {
        return IllegalStateException.class;
    }
}
