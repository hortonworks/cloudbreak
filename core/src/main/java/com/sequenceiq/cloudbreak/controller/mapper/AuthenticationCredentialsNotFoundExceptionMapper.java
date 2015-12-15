package com.sequenceiq.cloudbreak.controller.mapper;

import javax.ws.rs.core.Response;
import javax.ws.rs.ext.ExceptionMapper;
import javax.ws.rs.ext.Provider;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.authentication.AuthenticationCredentialsNotFoundException;

import com.sequenceiq.cloudbreak.controller.json.ExceptionResult;
import com.sequenceiq.cloudbreak.logger.MDCBuilder;

@Provider
public class AuthenticationCredentialsNotFoundExceptionMapper implements ExceptionMapper<AuthenticationCredentialsNotFoundException> {

    private static final Logger LOGGER = LoggerFactory.getLogger(AuthenticationCredentialsNotFoundExceptionMapper.class);

    @Override
    public Response toResponse(AuthenticationCredentialsNotFoundException exception) {
        MDCBuilder.buildMdcContext();
        LOGGER.error(exception.getMessage());
        return Response.status(Response.Status.UNAUTHORIZED).entity(new ExceptionResult(exception.getMessage()))
                .build();
    }
}
