package com.sequenceiq.environment.exception.mapper;

import static com.sequenceiq.environment.util.Validation.notNull;

import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Response;
import javax.ws.rs.ext.ExceptionMapper;

import org.springframework.stereotype.Component;

@Component
public class WebApplicationExceptionMapper implements ExceptionMapper<WebApplicationException> {

    @Override
    public Response toResponse(WebApplicationException exception) {
        notNull(exception, "exception");
        return exception.getResponse();
    }
}
