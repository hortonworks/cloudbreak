package com.sequenceiq.periscope.rest.mapper;

import javax.ws.rs.core.Response;
import javax.ws.rs.ext.Provider;

import org.springframework.http.converter.HttpMessageNotReadableException;

@Provider
public class HttpMessageNotReadableExceptionMapper extends BaseExceptionMapper<HttpMessageNotReadableException> {

    @Override
    Response.Status getResponseStatus() {
        return Response.Status.BAD_REQUEST;
    }
}