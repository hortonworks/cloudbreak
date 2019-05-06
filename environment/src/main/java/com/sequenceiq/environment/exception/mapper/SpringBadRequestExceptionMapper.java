package com.sequenceiq.environment.exception.mapper;

import javax.ws.rs.BadRequestException;
import javax.ws.rs.core.Response.Status;

import org.springframework.stereotype.Component;

@Component
public class SpringBadRequestExceptionMapper extends BaseExceptionMapper<BadRequestException> {

    @Override
    Status getResponseStatus() {
        return Status.BAD_REQUEST;
    }

    @Override
    Class<BadRequestException> getExceptionType() {
        return BadRequestException.class;
    }

}