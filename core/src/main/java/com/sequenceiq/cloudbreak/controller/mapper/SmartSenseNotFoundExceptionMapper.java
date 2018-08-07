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
    Class<SmartSenseConfigurationNotFoundException> getExceptionType() {
        return SmartSenseConfigurationNotFoundException.class;
    }

    @Override
    protected boolean logException() {
        return false;
    }
}
