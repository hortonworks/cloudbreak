package com.sequenceiq.datalake.controller.mapper;

import javax.ws.rs.BadRequestException;
import javax.ws.rs.core.Response;
import javax.ws.rs.ext.Provider;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.exception.mapper.BaseExceptionMapper;

@Provider
@Component
public class JaxRsBadRequestExceptionMapper extends BaseExceptionMapper<BadRequestException> {

    @Override
    public Response.Status getResponseStatus(BadRequestException exception) {
        return Response.Status.BAD_REQUEST;
    }

    @Override
    public Class<BadRequestException> getExceptionType() {
        return BadRequestException.class;
    }

}
