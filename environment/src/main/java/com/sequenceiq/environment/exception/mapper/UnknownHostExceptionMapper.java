package com.sequenceiq.environment.exception.mapper;

import java.net.UnknownHostException;

import javax.ws.rs.core.Response;

import org.springframework.stereotype.Component;

@Component
public class UnknownHostExceptionMapper extends EnvironmentBaseExceptionMapper<UnknownHostException> {

    @Override
    public Response.Status getResponseStatus(UnknownHostException exception) {
        return Response.Status.SERVICE_UNAVAILABLE;
    }

    @Override
    public Class<UnknownHostException> getExceptionType() {
        return UnknownHostException.class;
    }
}
