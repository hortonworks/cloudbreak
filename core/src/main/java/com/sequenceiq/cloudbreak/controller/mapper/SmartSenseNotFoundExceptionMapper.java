package com.sequenceiq.cloudbreak.controller.mapper;

import javax.ws.rs.core.Response;
import javax.ws.rs.ext.Provider;

import com.sequenceiq.cloudbreak.controller.SmartSenseNotFoundException;

@Provider
public class SmartSenseNotFoundExceptionMapper extends BaseExceptionMapper<SmartSenseNotFoundException> {

    @Override
    Response.Status getResponseStatus() {
        return Response.Status.NOT_FOUND;
    }

    @Override
    protected boolean logException() {
        return false;
    }
}
