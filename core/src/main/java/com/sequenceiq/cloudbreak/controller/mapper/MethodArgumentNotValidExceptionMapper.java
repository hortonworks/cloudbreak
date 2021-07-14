package com.sequenceiq.cloudbreak.controller.mapper;

import static ch.qos.logback.classic.Level.INFO;

import javax.ws.rs.core.Response.Status;

import org.springframework.messaging.handler.annotation.support.MethodArgumentNotValidException;
import org.springframework.stereotype.Component;
import org.springframework.validation.FieldError;

import com.sequenceiq.cloudbreak.exception.mapper.BaseExceptionMapper;
import com.sequenceiq.cloudbreak.json.ValidationResult;

import ch.qos.logback.classic.Level;

@Component
public class MethodArgumentNotValidExceptionMapper extends BaseExceptionMapper<MethodArgumentNotValidException> {

    @Override
    protected Level getLogLevel() {
        return INFO;
    }

    @Override
    protected Object getEntity(MethodArgumentNotValidException exception) {
        ValidationResult result = new ValidationResult();
        for (FieldError err : exception.getBindingResult().getFieldErrors()) {
            result.addValidationError(err.getField(), err.getDefaultMessage());
        }
        return result;
    }

    @Override
    public Status getResponseStatus(MethodArgumentNotValidException exception) {
        return Status.BAD_REQUEST;
    }

    @Override
    public Class<MethodArgumentNotValidException> getExceptionType() {
        return MethodArgumentNotValidException.class;
    }
}
