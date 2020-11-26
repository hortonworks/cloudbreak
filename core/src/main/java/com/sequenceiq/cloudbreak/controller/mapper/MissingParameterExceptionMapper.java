package com.sequenceiq.cloudbreak.controller.mapper;

import javax.ws.rs.core.Response.Status;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.service.stack.resource.definition.MissingParameterException;

@Component
public class MissingParameterExceptionMapper extends BaseExceptionMapper<MissingParameterException> {

    @Override
    Status getResponseStatus(MissingParameterException exception) {
        return Status.BAD_REQUEST;
    }

    @Override
    Class<MissingParameterException> getExceptionType() {
        return MissingParameterException.class;
    }
}
