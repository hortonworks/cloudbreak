package com.sequenceiq.periscope.controller.mapper;

import jakarta.ws.rs.core.Response.Status;
import jakarta.ws.rs.ext.Provider;

import org.springframework.http.converter.HttpMessageNotReadableException;

@Provider
public class HttpMessageNotReadableExceptionMapper extends BaseExceptionMapper<HttpMessageNotReadableException> {

    @Override
    Status getResponseStatus() {
        return Status.BAD_REQUEST;
    }
}
