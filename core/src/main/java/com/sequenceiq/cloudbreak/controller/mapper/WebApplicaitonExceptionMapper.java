package com.sequenceiq.cloudbreak.controller.mapper;

import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Response;

import org.springframework.stereotype.Component;

@Component
public class WebApplicaitonExceptionMapper implements TypeAwareExceptionMapper<WebApplicationException> {

    @Override
    public Response toResponse(WebApplicationException exception) {
        return exception.getResponse();
    }

    @Override
    public Class<WebApplicationException> supportedType() {
        return WebApplicationException.class;
    }
}
