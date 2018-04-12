package com.sequenceiq.cloudbreak.controller.mapper;

import com.sequenceiq.cloudbreak.service.smartsense.SmartSenseSubscriptionAccessDeniedException;

import javax.ws.rs.core.Response;

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
