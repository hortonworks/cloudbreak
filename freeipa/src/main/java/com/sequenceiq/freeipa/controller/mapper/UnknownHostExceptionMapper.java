package com.sequenceiq.freeipa.controller.mapper;

import java.net.UnknownHostException;

import javax.ws.rs.core.Response;

import org.springframework.stereotype.Component;

@Component
public class UnknownHostExceptionMapper extends BaseExceptionMapper<UnknownHostException> {

    @Override
    Response.Status getResponseStatus(UnknownHostException exception) {
        return Response.Status.SERVICE_UNAVAILABLE;
    }

    @Override
    Class<UnknownHostException> getExceptionType() {
        return UnknownHostException.class;
    }
}
