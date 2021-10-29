package com.sequenceiq.cloudbreak.exception.mapper;

import javax.ws.rs.core.Response.Status;

import org.springframework.stereotype.Component;
import org.springframework.web.HttpRequestMethodNotSupportedException;

@Component
public class HttpRequestMethodNotSupportedExceptionMapper extends BaseExceptionMapper<HttpRequestMethodNotSupportedException> {

    @Override
    protected String getErrorMessage(HttpRequestMethodNotSupportedException exception) {
        return "The requested http method is not supported on the resource.";
    }

    @Override
    public Status getResponseStatus(HttpRequestMethodNotSupportedException exception) {
        return Status.METHOD_NOT_ALLOWED;
    }

    @Override
    public Class<HttpRequestMethodNotSupportedException> getExceptionType() {
        return HttpRequestMethodNotSupportedException.class;
    }
}
