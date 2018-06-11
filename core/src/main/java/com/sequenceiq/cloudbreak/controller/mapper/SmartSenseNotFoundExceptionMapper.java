package com.sequenceiq.cloudbreak.controller.mapper;

import javax.ws.rs.core.Response.Status;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.controller.exception.SmartSenseConfigurationNotFoundException;

@Component
public class SmartSenseNotFoundExceptionMapper extends BaseExceptionMapper<SmartSenseConfigurationNotFoundException> {

    @Override
    Status getResponseStatus() {
        return Status.NOT_FOUND;
    }

    @Override
    protected boolean isLogException() {
        return false;
    }

    @Override
    public Class<SmartSenseConfigurationNotFoundException> supportedType() {
        return SmartSenseConfigurationNotFoundException.class;
    }
}
