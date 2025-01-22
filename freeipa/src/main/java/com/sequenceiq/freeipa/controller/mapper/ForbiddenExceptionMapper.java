package com.sequenceiq.freeipa.controller.mapper;

import jakarta.ws.rs.ForbiddenException;
import jakarta.ws.rs.core.Response.Status;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.exception.mapper.BaseExceptionMapper;

@Component
public class ForbiddenExceptionMapper extends BaseExceptionMapper<ForbiddenException> {

    @Override
    public Status getResponseStatus(ForbiddenException exception) {
        return Status.FORBIDDEN;
    }

    @Override
    public Class<ForbiddenException> getExceptionType() {
        return ForbiddenException.class;
    }
}
