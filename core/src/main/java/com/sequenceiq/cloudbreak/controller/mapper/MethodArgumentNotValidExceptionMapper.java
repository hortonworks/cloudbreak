package com.sequenceiq.cloudbreak.controller.mapper;

import static ch.qos.logback.classic.Level.INFO;

import java.util.ArrayList;
import java.util.List;

import javax.ws.rs.core.Response.Status;

import org.springframework.messaging.handler.annotation.support.MethodArgumentNotValidException;
import org.springframework.stereotype.Component;
import org.springframework.validation.FieldError;

import com.sequenceiq.cloudbreak.exception.mapper.BaseExceptionMapper;
import com.sequenceiq.cloudbreak.exception.mapper.ValidationResultResponse;

import ch.qos.logback.classic.Level;

@Component
public class MethodArgumentNotValidExceptionMapper extends BaseExceptionMapper<MethodArgumentNotValidException> {

    @Override
    protected Level getLogLevel() {
        return INFO;
    }

    @Override
    protected Object getPayload(MethodArgumentNotValidException exception) {
        List<ValidationResultResponse> results = new ArrayList<>();
        for (FieldError err : exception.getBindingResult().getFieldErrors()) {
            results.add(new ValidationResultResponse(err.getField(), err.getDefaultMessage()));
        }
        return results;
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
