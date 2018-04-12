package com.sequenceiq.cloudbreak.controller.mapper;

import javax.ws.rs.core.Response.Status;
import javax.ws.rs.ext.Provider;

import com.sequenceiq.cloudbreak.service.stack.flow.TerminationFailedException;

@Provider
public class TerminationFailedExceptionMapper extends SendNotificationExceptionMapper<TerminationFailedException> {

    @Override
    Status getResponseStatus() {
        return Status.BAD_REQUEST;
    }
}
