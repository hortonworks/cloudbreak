package com.sequenceiq.periscope.controller.mapper;

import jakarta.ws.rs.ForbiddenException;
import jakarta.ws.rs.core.Response.Status;
import jakarta.ws.rs.ext.Provider;

@Provider
public class ForbiddenExceptionMapper extends BaseExceptionMapper<ForbiddenException> {

    @Override
    Status getResponseStatus() {
        return Status.FORBIDDEN;
    }
}
