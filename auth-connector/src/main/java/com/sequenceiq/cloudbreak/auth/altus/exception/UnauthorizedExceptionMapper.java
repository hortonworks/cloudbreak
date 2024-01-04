package com.sequenceiq.cloudbreak.auth.altus.exception;

import jakarta.ws.rs.core.Response;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.exception.mapper.BaseExceptionMapper;

@Component
public class UnauthorizedExceptionMapper extends BaseExceptionMapper<UnauthorizedException> {

    @Override
    public Response.Status getResponseStatus(UnauthorizedException exception) {
        return Response.Status.UNAUTHORIZED;
    }

    @Override
    public Class<UnauthorizedException> getExceptionType() {
        return UnauthorizedException.class;
    }
}
