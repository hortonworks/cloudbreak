package com.sequenceiq.cloudbreak.controller.mapper;

import jakarta.ws.rs.NotFoundException;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.UriInfo;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.exception.mapper.BaseExceptionMapper;

import ch.qos.logback.classic.Level;

@Component
public class JaxRsNotFoundExceptionMapper extends BaseExceptionMapper<NotFoundException> {

    @Context
    private UriInfo uriInfo;

    @Override
    public Response.Status getResponseStatus(NotFoundException notFoundException) {
        return Response.Status.NOT_FOUND;
    }

    @Override
    public Class<NotFoundException> getExceptionType() {
        return NotFoundException.class;
    }

    @Override
    protected String getErrorMessage(NotFoundException throwable) {
        String absolutePath = uriInfo.getRequestUri().getPath();
        String errorMessage = super.getErrorMessage(throwable);
        return String.format("Couldn't find the specified resource on path '%s', error message: %s", absolutePath, errorMessage);
    }

    @Override
    protected Level getLogLevel() {
        return Level.INFO;
    }
}
