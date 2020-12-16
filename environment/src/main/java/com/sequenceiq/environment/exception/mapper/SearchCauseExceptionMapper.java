package com.sequenceiq.environment.exception.mapper;

import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response;
import javax.ws.rs.ext.Providers;

public abstract class SearchCauseExceptionMapper<T extends Throwable> extends BaseExceptionMapper<T> {

    @Context
    private Providers providers;

    @Override
    Response.Status getResponseStatus(T exception) {
        Response.Status defaultResponse = Response.Status.BAD_REQUEST;
        if (exception.getCause() != null && Exception.class == exception.getCause().getClass()) {
            return defaultResponse;
        }
        BaseExceptionMapper exceptionMapper = (BaseExceptionMapper<? extends Throwable>) providers.getExceptionMapper(exception.getCause().getClass());
        if (exceptionMapper == null) {
            return defaultResponse;
        }
        return exceptionMapper.getResponseStatus(exception.getCause());
    }
}
