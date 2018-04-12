package com.sequenceiq.cloudbreak.controller.mapper;

import static javax.ws.rs.core.Response.Status.BAD_REQUEST;

import javax.ws.rs.core.Response;

import com.sequenceiq.cloudbreak.service.stack.GetCloudParameterException;

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
