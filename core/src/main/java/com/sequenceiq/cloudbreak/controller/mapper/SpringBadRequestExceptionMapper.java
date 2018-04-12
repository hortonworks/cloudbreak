package com.sequenceiq.cloudbreak.controller.mapper;

import com.sequenceiq.cloudbreak.controller.BadRequestException;

import javax.ws.rs.core.Response.Status;
import javax.ws.rs.ext.Provider;

@Provider
public class SpringBadRequestExceptionMapper extends BaseExceptionMapper<BadRequestException> {

    @Override
    Status getResponseStatus() {
        return Status.BAD_REQUEST;
    }
}