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
    Class<SmartSenseSubscriptionAccessDeniedException> getExceptionType() {
        return SmartSenseSubscriptionAccessDeniedException.class;
    }

    @Override
    protected boolean logException() {
        return false;
    }

}
