package com.sequenceiq.remoteenvironment.controller.mapper;

import jakarta.ws.rs.core.Response.Status;
import jakarta.ws.rs.ext.Provider;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.common.exception.BadRequestException;
import com.sequenceiq.cloudbreak.exception.mapper.BaseExceptionMapper;

@Provider
@Component
public class BadRequestExceptionMapper extends BaseExceptionMapper<BadRequestException> {

    @Override
    public Status getResponseStatus(BadRequestException exception) {
        return Status.BAD_REQUEST;
    }

    @Override
    public Class<BadRequestException> getExceptionType() {
        return BadRequestException.class;
    }

}
