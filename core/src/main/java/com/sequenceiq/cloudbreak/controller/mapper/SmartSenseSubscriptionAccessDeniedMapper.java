package com.sequenceiq.cloudbreak.controller.mapper;

import javax.ws.rs.core.Response;

import com.sequenceiq.cloudbreak.service.smartsense.SmartSenseSubscriptionAccessDeniedException;

public class SmartSenseSubscriptionAccessDeniedMapper extends BaseExceptionMapper<SmartSenseSubscriptionAccessDeniedException> {

    @Override
    Response.Status getResponseStatus() {
        return Response.Status.FORBIDDEN;
    }

    @Override
    protected boolean logException() {
        return false;
    }

}
