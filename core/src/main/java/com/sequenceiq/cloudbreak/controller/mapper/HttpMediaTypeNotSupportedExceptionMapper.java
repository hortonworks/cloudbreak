package com.sequenceiq.cloudbreak.controller.mapper;

import javax.ws.rs.core.Response.Status;

import org.springframework.stereotype.Component;
import org.springframework.web.HttpMediaTypeNotSupportedException;

@Component
public class HttpMediaTypeNotSupportedExceptionMapper extends BaseExceptionMapper<HttpMediaTypeNotSupportedException> {

    @Override
    Status getResponseStatus(HttpMediaTypeNotSupportedException exception) {
        return Status.NOT_ACCEPTABLE;
    }

    @Override
    Class<HttpMediaTypeNotSupportedException> getExceptionType() {
        return HttpMediaTypeNotSupportedException.class;
    }
}
