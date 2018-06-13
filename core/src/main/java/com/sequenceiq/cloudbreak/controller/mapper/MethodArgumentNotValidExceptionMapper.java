package com.sequenceiq.cloudbreak.controller.mapper;

import javax.ws.rs.core.Response.Status;

import org.springframework.messaging.handler.annotation.support.MethodArgumentNotValidException;
import org.springframework.stereotype.Component;
import org.springframework.validation.FieldError;

import com.sequenceiq.cloudbreak.controller.json.ValidationResult;

@Component
public class MethodArgumentNotValidExceptionMapper extends BaseExceptionMapper<MethodArgumentNotValidException> {

    @Override
    protected Object getEntity(MethodArgumentNotValidException exception) {
        ValidationResult result = new ValidationResult();
        for (FieldError err : exception.getBindingResult().getFieldErrors()) {
            result.addValidationError(err.getField(), err.getDefaultMessage());
        }
        return result;
    }

    @Override
    Status getResponseStatus() {
        return Status.BAD_REQUEST;
    }

    @Override
    Class<MethodArgumentNotValidException> getExceptionType() {
        return MethodArgumentNotValidException.class;
    }
}
