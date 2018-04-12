package com.sequenceiq.cloudbreak.controller.mapper;

import com.sequenceiq.cloudbreak.controller.SmartSenseConfigurationNotFoundException;

import javax.ws.rs.core.Response.Status;
import javax.ws.rs.ext.Provider;

@Provider
public class SmartSenseNotFoundExceptionMapper extends BaseExceptionMapper<SmartSenseConfigurationNotFoundException> {

    @Override
    Status getResponseStatus() {
        return Status.NOT_FOUND;
    }

    @Override
    protected boolean logException() {
        return false;
    }
}
