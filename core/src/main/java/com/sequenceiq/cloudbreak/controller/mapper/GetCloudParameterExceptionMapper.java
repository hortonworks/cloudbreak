package com.sequenceiq.cloudbreak.controller.mapper;

import com.sequenceiq.cloudbreak.service.stack.GetCloudParameterException;

import javax.ws.rs.core.Response;

import static javax.ws.rs.core.Response.Status.BAD_REQUEST;

public class GetCloudParameterExceptionMapper extends BaseExceptionMapper<GetCloudParameterException> {

    @Override
    Response.Status getResponseStatus() {
        return BAD_REQUEST;
    }

    @Override
    protected boolean logException() {
        return false;
    }
}
