package com.sequenceiq.cloudbreak.controller.mapper;

import javax.ws.rs.core.Response.Status;
import javax.ws.rs.ext.Provider;

import com.sequenceiq.cloudbreak.controller.NotFoundException;

@Provider
public class NotFoundExceptionMapper extends BaseExceptionMapper<NotFoundException> {

    @Override
    Status getResponseStatus() {
        return Status.NOT_FOUND;
    }
}
