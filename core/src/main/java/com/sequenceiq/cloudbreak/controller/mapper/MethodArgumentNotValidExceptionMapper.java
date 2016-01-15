package com.sequenceiq.cloudbreak.controller.mapper;

import javax.ws.rs.core.Response;
import javax.ws.rs.ext.ExceptionMapper;
import javax.ws.rs.ext.Provider;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.messaging.handler.annotation.support.MethodArgumentNotValidException;
import org.springframework.validation.FieldError;

import com.sequenceiq.cloudbreak.controller.json.ValidationResult;
import com.sequenceiq.cloudbreak.logger.MDCBuilder;

@Provider
public class MethodArgumentNotValidExceptionMapper implements ExceptionMapper<MethodArgumentNotValidException> {

    private static final Logger LOGGER = LoggerFactory.getLogger(NotFoundExceptionMapper.class);

    @Override
    public Response toResponse(MethodArgumentNotValidException exception) {
        MDCBuilder.buildMdcContext();
        LOGGER.error(exception.getMessage());

        ValidationResult result = new ValidationResult();
        for (FieldError err : exception.getBindingResult().getFieldErrors()) {
            result.addValidationError(err.getField(), err.getDefaultMessage());
        }

        return Response.status(Response.Status.BAD_REQUEST).entity(result).build();
    }
}
