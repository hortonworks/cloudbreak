package com.sequenceiq.freeipa.controller.mapper;

import javax.ws.rs.BadRequestException;
import javax.ws.rs.core.Response.Status;

import org.springframework.stereotype.Component;

@Component
public class BadRequestExceptionMapper extends BaseExceptionMapper<BadRequestException> {

    @Override
    protected String getErrorMessage(BadRequestException exception) {
        return exception.getMessage();
    }

    @Override
    Status getResponseStatus(BadRequestException exception) {
        return Status.BAD_REQUEST;
    }

    @Override
    Class<BadRequestException> getExceptionType() {
        return BadRequestException.class;
    }
}