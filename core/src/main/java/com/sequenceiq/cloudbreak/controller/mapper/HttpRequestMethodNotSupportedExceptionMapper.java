package com.sequenceiq.cloudbreak.controller.mapper;

import javax.ws.rs.core.Response.Status;
import javax.ws.rs.ext.Provider;

import org.springframework.web.HttpRequestMethodNotSupportedException;

import com.sequenceiq.cloudbreak.controller.json.ExceptionResult;

@Provider
public class HttpRequestMethodNotSupportedExceptionMapper extends BaseExceptionMapper<HttpRequestMethodNotSupportedException> {

    @Override
    protected Object getEntity(HttpRequestMethodNotSupportedException exception) {
        return new ExceptionResult("The requested http method is not supported on the resource.");
    }

    @Override
    Status getResponseStatus() {
        return Status.BAD_REQUEST;
    }
}
