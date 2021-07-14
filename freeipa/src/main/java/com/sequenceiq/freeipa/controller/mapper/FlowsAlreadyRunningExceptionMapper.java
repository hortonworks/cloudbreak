package com.sequenceiq.freeipa.controller.mapper;

import javax.ws.rs.core.Response.Status;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.exception.FlowsAlreadyRunningException;
import com.sequenceiq.cloudbreak.exception.mapper.BaseExceptionMapper;

@Component
public class FlowsAlreadyRunningExceptionMapper extends BaseExceptionMapper<FlowsAlreadyRunningException> {

    @Override
    public Status getResponseStatus(FlowsAlreadyRunningException exception) {
        return Status.CONFLICT;
    }

    @Override
    public Class<FlowsAlreadyRunningException> getExceptionType() {
        return FlowsAlreadyRunningException.class;
    }
}
