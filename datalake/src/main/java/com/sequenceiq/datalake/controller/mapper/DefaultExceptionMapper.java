package com.sequenceiq.datalake.controller.mapper;

import javax.ws.rs.core.Response.Status;
import javax.ws.rs.ext.Provider;

import org.apache.commons.lang3.exception.ExceptionUtils;
import org.springframework.stereotype.Component;

@Provider
@Component
public class DefaultExceptionMapper extends BaseExceptionMapper<Exception> {

    @Override
    protected Object getEntity(Exception exception) {
        return ExceptionUtils.getStackTrace(exception);
    }

    @Override
    Status getResponseStatus(Exception exception) {
        return Status.INTERNAL_SERVER_ERROR;
    }

    @Override
    Class<Exception> getExceptionType() {
        return Exception.class;
    }
}
