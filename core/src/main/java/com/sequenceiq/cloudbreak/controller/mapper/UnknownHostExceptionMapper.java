package com.sequenceiq.cloudbreak.controller.mapper;

import java.net.UnknownHostException;

import javax.ws.rs.core.Response;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.exception.mapper.BaseExceptionMapper;

@Component
public class UnknownHostExceptionMapper extends BaseExceptionMapper<UnknownHostException> {

    @Override
    public Response.Status getResponseStatus(UnknownHostException exception) {
        return Response.Status.SERVICE_UNAVAILABLE;
    }

    @Override
    public Class<UnknownHostException> getExceptionType() {
        return UnknownHostException.class;
    }
}
