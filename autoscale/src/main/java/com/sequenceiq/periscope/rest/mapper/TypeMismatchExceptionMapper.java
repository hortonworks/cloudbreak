package com.sequenceiq.periscope.rest.mapper;

import javax.ws.rs.core.Response;
import javax.ws.rs.ext.ExceptionMapper;
import javax.ws.rs.ext.Provider;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.TypeMismatchException;

import com.sequenceiq.periscope.api.model.ExceptionResult;

@Provider
public class TypeMismatchExceptionMapper implements ExceptionMapper<TypeMismatchException> {

    private static final Logger LOGGER = LoggerFactory.getLogger(TypeMismatchExceptionMapper.class);

    @Override
    public Response toResponse(TypeMismatchException exception) {
        LOGGER.error(exception.getMessage(), exception);
        return Response.status(Response.Status.BAD_REQUEST).entity(new ExceptionResult(exception.getMessage()))
                .build();
    }
}