package com.sequenceiq.cloudbreak.controller.mapper;

import javax.ws.rs.NotFoundException;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;

import org.springframework.stereotype.Component;

import ch.qos.logback.classic.Level;

@Component
public class JaxRsNotFoundExceptionMapper extends BaseExceptionMapper<NotFoundException> {

    @Context
    private UriInfo uriInfo;

    @Override
    Response.Status getResponseStatus(NotFoundException notFoundException) {
        return Response.Status.NOT_FOUND;
    }

    @Override
    Class<NotFoundException> getExceptionType() {
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
