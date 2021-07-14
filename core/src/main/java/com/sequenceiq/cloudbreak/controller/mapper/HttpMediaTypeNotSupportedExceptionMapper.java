package com.sequenceiq.cloudbreak.controller.mapper;

import javax.ws.rs.core.Response.Status;

import org.springframework.stereotype.Component;
import org.springframework.web.HttpMediaTypeNotSupportedException;

import com.sequenceiq.cloudbreak.exception.mapper.BaseExceptionMapper;

@Component
public class HttpMediaTypeNotSupportedExceptionMapper extends BaseExceptionMapper<HttpMediaTypeNotSupportedException> {

    @Override
    public Status getResponseStatus(HttpMediaTypeNotSupportedException exception) {
        return Status.NOT_ACCEPTABLE;
    }

    @Override
    public Class<HttpMediaTypeNotSupportedException> getExceptionType() {
        return HttpMediaTypeNotSupportedException.class;
    }
}
