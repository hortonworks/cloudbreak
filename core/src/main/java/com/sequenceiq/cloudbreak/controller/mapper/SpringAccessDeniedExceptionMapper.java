package com.sequenceiq.cloudbreak.controller.mapper;

import javax.ws.rs.core.Response.Status;
import javax.ws.rs.ext.Provider;

import org.springframework.security.access.AccessDeniedException;

@Provider
public class SpringAccessDeniedExceptionMapper extends BaseExceptionMapper<AccessDeniedException> {

    @Override
    Status getResponseStatus() {
        return Status.FORBIDDEN;
    }
}
