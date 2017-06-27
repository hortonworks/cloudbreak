package com.sequenceiq.cloudbreak.controller.mapper;

import javax.ws.rs.core.Response;
import javax.ws.rs.ext.Provider;

import com.sequenceiq.cloudbreak.service.stack.flow.TerminationFailedException;

@Provider
public class TerminationFailedExceptionMapper extends SendNotificationExceptionMapper<TerminationFailedException> {

    @Override
    Response.Status getResponseStatus() {
        return Response.Status.BAD_REQUEST;
    }
}
