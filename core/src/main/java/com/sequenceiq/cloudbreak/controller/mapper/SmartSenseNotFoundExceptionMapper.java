package com.sequenceiq.cloudbreak.controller.mapper;

import javax.ws.rs.core.Response.Status;
import javax.ws.rs.ext.Provider;

import com.sequenceiq.cloudbreak.controller.SmartSenseNotFoundException;

@Provider
public class SmartSenseNotFoundExceptionMapper extends BaseExceptionMapper<SmartSenseNotFoundException> {

    @Override
    Status getResponseStatus() {
        return Status.NOT_FOUND;
    }

    @Override
    protected boolean logException() {
        return false;
    }
}
