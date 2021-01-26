package com.sequenceiq.redbeams.controller.mapper;

import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

import org.springframework.stereotype.Component;

import com.sequenceiq.redbeams.exception.ConflictException;

@Component
public class ConflictExceptionMapper extends BaseExceptionMapper<ConflictException> {

    @Override
    Status getResponseStatus(ConflictException exception) {
        return Status.CONFLICT;
    }

    @Override
    Class<ConflictException> getExceptionType() {
        return ConflictException.class;
    }

    @Override
    public Response toResponse(ConflictException exception) {
        return Response.status(getResponseStatus(exception)).entity(getEntity(exception)).build();
    }
}
