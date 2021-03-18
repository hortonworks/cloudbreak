package com.sequenceiq.freeipa.controller.mapper;

import javax.ws.rs.core.Response.Status;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.exception.FlowsAlreadyRunningException;

@Component
public class FlowsAlreadyRunningExceptionMapper extends BaseExceptionMapper<FlowsAlreadyRunningException> {

    @Override
    Status getResponseStatus(FlowsAlreadyRunningException exception) {
        return Status.CONFLICT;
    }

    @Override
    Class<FlowsAlreadyRunningException> getExceptionType() {
        return FlowsAlreadyRunningException.class;
    }
}
