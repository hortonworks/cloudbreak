package com.sequenceiq.cloudbreak.controller.mapper;

import javax.ws.rs.core.Response.Status;

import org.springframework.stereotype.Component;
import org.springframework.web.HttpRequestMethodNotSupportedException;

import com.sequenceiq.cloudbreak.common.exception.ExceptionResponse;
import com.sequenceiq.cloudbreak.exception.mapper.BaseExceptionMapper;

@Component
public class HttpRequestMethodNotSupportedExceptionMapper extends BaseExceptionMapper<HttpRequestMethodNotSupportedException> {

    @Override
    protected Object getEntity(HttpRequestMethodNotSupportedException exception) {
        return new ExceptionResponse("The requested http method is not supported on the resource.");
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
