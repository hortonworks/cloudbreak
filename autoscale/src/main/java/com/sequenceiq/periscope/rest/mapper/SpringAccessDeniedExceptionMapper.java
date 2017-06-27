package com.sequenceiq.periscope.rest.mapper;

import javax.ws.rs.core.Response;
import javax.ws.rs.ext.Provider;

import org.springframework.security.access.AccessDeniedException;

@Provider
public class SpringAccessDeniedExceptionMapper extends BaseExceptionMapper<AccessDeniedException> {

    @Override
    Response.Status getResponseStatus() {
        return Response.Status.FORBIDDEN;
    }
}
