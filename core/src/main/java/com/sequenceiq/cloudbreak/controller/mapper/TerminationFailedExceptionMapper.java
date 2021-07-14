package com.sequenceiq.cloudbreak.controller.mapper;

import javax.ws.rs.core.Response.Status;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.service.stack.flow.TerminationFailedException;

@Component
public class TerminationFailedExceptionMapper extends SendNotificationExceptionMapper<TerminationFailedException> {

    @Override
    public Status getResponseStatus(TerminationFailedException exception) {
        return Status.BAD_REQUEST;
    }

    @Override
    public Class<TerminationFailedException> getExceptionType() {
        return TerminationFailedException.class;
    }
}
