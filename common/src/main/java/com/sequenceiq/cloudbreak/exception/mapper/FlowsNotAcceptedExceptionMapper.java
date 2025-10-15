package com.sequenceiq.cloudbreak.exception.mapper;

import jakarta.ws.rs.core.Response.Status;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.exception.FlowNotAcceptedException;

@Component
public class FlowsNotAcceptedExceptionMapper extends BaseExceptionMapper<FlowNotAcceptedException> {

    @Override
    public Status getResponseStatus(FlowNotAcceptedException exception) {
        return Status.CONFLICT;
    }

    @Override
    public Class<FlowNotAcceptedException> getExceptionType() {
        return FlowNotAcceptedException.class;
    }
}
