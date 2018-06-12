package com.sequenceiq.cloudbreak.controller.mapper;

import javax.ws.rs.core.Response.Status;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.service.smartsense.SmartSenseSubscriptionAccessDeniedException;

@Component
public class SmartSenseSubscriptionAccessDeniedMapper extends BaseExceptionMapper<SmartSenseSubscriptionAccessDeniedException> {

    @Override
    Status getResponseStatus() {
        return Status.FORBIDDEN;
    }

    @Override
    protected boolean isLogException() {
        return false;
    }

    @Override
    public Class<SmartSenseSubscriptionAccessDeniedException> supportedType() {
        return SmartSenseSubscriptionAccessDeniedException.class;
    }
}
