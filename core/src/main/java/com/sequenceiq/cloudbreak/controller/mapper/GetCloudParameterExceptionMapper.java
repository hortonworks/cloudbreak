package com.sequenceiq.cloudbreak.controller.mapper;

import static javax.ws.rs.core.Response.Status.BAD_REQUEST;

import javax.ws.rs.core.Response.Status;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.service.stack.GetCloudParameterException;

@Component
public class GetCloudParameterExceptionMapper extends BaseExceptionMapper<GetCloudParameterException> {

    @Override
    Status getResponseStatus() {
        return BAD_REQUEST;
    }

    @Override
    Class<GetCloudParameterException> getExceptionType() {
        return GetCloudParameterException.class;
    }

    @Override
    protected boolean logException() {
        return false;
    }
}
