package com.sequenceiq.maintenance.controller.mapper;

import jakarta.ws.rs.core.Response.Status;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.exception.mapper.BaseExceptionMapper;
import com.sequenceiq.maintenance.exception.ConflictException;

@Component
public class ConflictExceptionMapper extends BaseExceptionMapper<ConflictException> {

    @Override
    public Status getResponseStatus(ConflictException exception) {
        return Status.CONFLICT;
    }

    @Override
    public Class<ConflictException> getExceptionType() {
        return ConflictException.class;
    }
}
