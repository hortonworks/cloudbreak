package com.sequenceiq.freeipa.controller.mapper;

import javax.persistence.EntityNotFoundException;
import javax.ws.rs.core.Response.Status;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.exception.mapper.BaseExceptionMapper;

@Component
public class EntityNotFoundExceptionMapper extends BaseExceptionMapper<EntityNotFoundException> {

    @Override
    public Status getResponseStatus(EntityNotFoundException exception) {
        return Status.NOT_FOUND;
    }

    @Override
    public Class<EntityNotFoundException> getExceptionType() {
        return EntityNotFoundException.class;
    }
}
