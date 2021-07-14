package com.sequenceiq.cloudbreak.controller.mapper;

import static ch.qos.logback.classic.Level.INFO;

import javax.ws.rs.core.Response.Status;

import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.exception.mapper.BaseExceptionMapper;

import ch.qos.logback.classic.Level;

@Component
public class HttpMessageNotReadableExceptionMapper extends BaseExceptionMapper<HttpMessageNotReadableException> {

    @Override
    protected Level getLogLevel() {
        return INFO;
    }

    @Override
    public Status getResponseStatus(HttpMessageNotReadableException exception) {
        return Status.BAD_REQUEST;
    }

    @Override
    public Class<HttpMessageNotReadableException> getExceptionType() {
        return HttpMessageNotReadableException.class;
    }
}