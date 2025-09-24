package com.sequenceiq.freeipa.controller.mapper;

import jakarta.ws.rs.BadRequestException;
import jakarta.ws.rs.core.Response.Status;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.exception.mapper.BaseExceptionMapper;

@Component
public class JaxWsBadRequestExceptionMapper extends BaseExceptionMapper<BadRequestException> {

    @Override
    public Status getResponseStatus(BadRequestException exception) {
        return Status.BAD_REQUEST;
    }

    @Override
    public Class<BadRequestException> getExceptionType() {
        return BadRequestException.class;
    }
}
