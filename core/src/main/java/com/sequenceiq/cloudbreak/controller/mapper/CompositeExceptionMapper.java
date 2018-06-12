package com.sequenceiq.cloudbreak.controller.mapper;


import java.util.Map;

import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;
import javax.ws.rs.ext.ExceptionMapper;
import javax.ws.rs.ext.Provider;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.common.exception.WrapperException;

@Provider
@Component
public class CompositeExceptionMapper implements ExceptionMapper<Throwable> {

    private static final Logger LOGGER = LoggerFactory.getLogger(CompositeExceptionMapper.class);

    private final Map<Class<? extends Throwable>, TypeAwareExceptionMapper<Throwable>> exceptionMappers;

    public CompositeExceptionMapper(Map<Class<? extends Throwable>, TypeAwareExceptionMapper<Throwable>> exceptionMappers) {
        this.exceptionMappers = exceptionMappers;
    }

    @Override
    public Response toResponse(Throwable throwable) {
        if (throwable instanceof WrapperException) {
            throwable = ((WrapperException) throwable).getRootCause();
        }
        if (exceptionMappers.containsKey(throwable.getClass())) {
            return exceptionMappers.get(throwable.getClass()).toResponse(throwable);
        }
        LOGGER.error(throwable.getMessage(), throwable);
        return Response
                .status(Status.INTERNAL_SERVER_ERROR)
                .entity("Internal server error: " + throwable.getMessage())
                .build();
    }
}
