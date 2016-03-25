package com.sequenceiq.periscope.rest.mapper;

import javax.ws.rs.core.Response;
import javax.ws.rs.ext.ExceptionMapper;
import javax.ws.rs.ext.Provider;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.access.AccessDeniedException;

import com.sequenceiq.periscope.api.model.ExceptionResult;

@Provider
public class SpringAccessDeniedExceptionMapper implements ExceptionMapper<AccessDeniedException> {

    private static final Logger LOGGER = LoggerFactory.getLogger(SpringAccessDeniedExceptionMapper.class);

    @Override
    public Response toResponse(AccessDeniedException exception) {
        LOGGER.error(exception.getMessage(), exception);
        return Response.status(Response.Status.FORBIDDEN).entity(new ExceptionResult(exception.getMessage()))
                .build();
    }
}
