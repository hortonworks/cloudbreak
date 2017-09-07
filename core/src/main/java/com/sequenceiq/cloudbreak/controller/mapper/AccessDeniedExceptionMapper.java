package com.sequenceiq.cloudbreak.controller.mapper;

import java.nio.file.AccessDeniedException;

import javax.ws.rs.core.Response.Status;
import javax.ws.rs.ext.Provider;

@Provider
public class AccessDeniedExceptionMapper extends BaseExceptionMapper<AccessDeniedException> {

    @Override
    Status getResponseStatus() {
        return Status.FORBIDDEN;
    }
}
