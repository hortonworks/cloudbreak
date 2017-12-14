package com.sequenceiq.periscope.controller.mapper;

import javax.ws.rs.core.Response.Status;
import javax.ws.rs.ext.Provider;

import com.sequenceiq.periscope.service.NotFoundException;

@Provider
public class NotFoundExceptionMapper extends BaseExceptionMapper<NotFoundException> {

    @Override
    Status getResponseStatus() {
        return Status.NOT_FOUND;
    }
}
