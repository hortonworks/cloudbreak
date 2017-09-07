package com.sequenceiq.periscope.rest.mapper;

import javax.ws.rs.core.Response.Status;
import javax.ws.rs.ext.Provider;

import org.springframework.http.converter.HttpMessageNotReadableException;

@Provider
public class HttpMessageNotReadableExceptionMapper extends BaseExceptionMapper<HttpMessageNotReadableException> {

    @Override
    Status getResponseStatus() {
        return Status.BAD_REQUEST;
    }
}