package com.sequenceiq.periscope.rest.mapper;

import javax.ws.rs.core.Response;
import javax.ws.rs.ext.Provider;

import com.sequenceiq.periscope.service.NotFoundException;

@Provider
public class NotFoundExceptionMapper extends BaseExceptionMapper<NotFoundException> {

    @Override
    Response.Status getResponseStatus() {
        return Response.Status.NOT_FOUND;
    }
}
