package com.sequenceiq.cloudbreak.controller.mapper;

import javax.persistence.EntityNotFoundException;
import javax.ws.rs.core.Response.Status;

import org.springframework.stereotype.Component;

@Component
public class EntityNotFoundExceptionMapper extends BaseExceptionMapper<EntityNotFoundException> {

    @Override
    Status getResponseStatus(EntityNotFoundException exception) {
        return Status.NOT_FOUND;
    }

    @Override
    Class<EntityNotFoundException> getExceptionType() {
        return EntityNotFoundException.class;
    }
}
