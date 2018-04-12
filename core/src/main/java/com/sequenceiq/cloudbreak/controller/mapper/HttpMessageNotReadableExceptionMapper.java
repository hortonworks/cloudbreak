package com.sequenceiq.cloudbreak.controller.mapper;

import org.springframework.http.converter.HttpMessageNotReadableException;

import javax.ws.rs.core.Response.Status;
import javax.ws.rs.ext.Provider;

@Provider
public class HttpMessageNotReadableExceptionMapper extends BaseExceptionMapper<HttpMessageNotReadableException> {

    @Override
    Status getResponseStatus() {
        return Status.BAD_REQUEST;
    }
}