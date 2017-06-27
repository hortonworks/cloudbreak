package com.sequenceiq.cloudbreak.controller.mapper;

import javax.persistence.EntityNotFoundException;
import javax.ws.rs.core.Response;
import javax.ws.rs.ext.Provider;

@Provider
public class EntityNotFoundExceptionMapper extends BaseExceptionMapper<EntityNotFoundException> {

    @Override
    Response.Status getResponseStatus() {
        return Response.Status.NOT_FOUND;
    }
}
