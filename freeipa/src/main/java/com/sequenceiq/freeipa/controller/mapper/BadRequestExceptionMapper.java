package com.sequenceiq.freeipa.controller.mapper;

import javax.ws.rs.core.Response;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.common.exception.BadRequestException;
import com.sequenceiq.cloudbreak.exception.mapper.BaseExceptionMapper;

@Component
public class BadRequestExceptionMapper extends BaseExceptionMapper<BadRequestException> {

    @Override
    protected String getErrorMessage(BadRequestException exception) {
        return exception.getMessage();
    }

    @Override
    public Response.Status getResponseStatus(BadRequestException exception) {
        return Response.Status.BAD_REQUEST;
    }

    @Override
    public Class<BadRequestException> getExceptionType() {
        return BadRequestException.class;
    }
}
