package com.sequenceiq.environment.exception.mapper;

import static com.sequenceiq.environment.util.Validation.notNull;

import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Response;
import javax.ws.rs.ext.ExceptionMapper;
import javax.ws.rs.ext.Provider;

@Provider
public class WebApplicaitonExceptionMapper implements ExceptionMapper<WebApplicationException> {

    @Override
    public Response toResponse(WebApplicationException exception) {
        notNull(exception, "exception");
        return exception.getResponse();
    }

}
