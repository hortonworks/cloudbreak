package com.sequenceiq.cloudbreak.controller.mapper;

import javax.ws.rs.core.Response;
import javax.ws.rs.ext.Provider;

import com.sequenceiq.cloudbreak.controller.NotFoundException;

@Provider
public class NotFoundExceptionMapper extends BaseExceptionMapper<NotFoundException> {

    @Override
    Response.Status getResponseStatus() {
        return Response.Status.NOT_FOUND;
    }
}
