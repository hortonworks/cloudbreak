package com.sequenceiq.environment.exception.mapper;

import javax.ws.rs.core.Response.Status;

import org.springframework.stereotype.Component;

@Component
public class DefaultExceptionMapper extends SearchCauseExceptionMapper<Exception> {

    @Override
    Status getResponseStatus(Exception exception) {
        return Status.INTERNAL_SERVER_ERROR;
    }

    @Override
    Class<Exception> getExceptionType() {
        return Exception.class;
    }

}
