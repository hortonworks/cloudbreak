package com.sequenceiq.cloudbreak.controller.mapper;

import com.sequenceiq.cloudbreak.controller.json.ExceptionResult;
import org.springframework.web.HttpRequestMethodNotSupportedException;

import javax.ws.rs.core.Response.Status;
import javax.ws.rs.ext.Provider;

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
