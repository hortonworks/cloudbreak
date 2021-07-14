package com.sequenceiq.flow.core.exception;

import javax.ws.rs.core.Response;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.exception.mapper.BaseExceptionMapper;

@Component
public class FlowNotTriggerableExceptionMapper extends BaseExceptionMapper<FlowNotTriggerableException> {

    @Override
    public Response.Status getResponseStatus(FlowNotTriggerableException exception) {
        return Response.Status.CONFLICT;
    }

    @Override
    public Class<FlowNotTriggerableException> getExceptionType() {
        return FlowNotTriggerableException.class;
    }
}
