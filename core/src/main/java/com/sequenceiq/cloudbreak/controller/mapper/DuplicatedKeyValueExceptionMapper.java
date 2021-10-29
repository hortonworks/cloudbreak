package com.sequenceiq.cloudbreak.controller.mapper;

import static ch.qos.logback.classic.Level.INFO;

import javax.ws.rs.core.Response.Status;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.exception.mapper.BaseExceptionMapper;
import com.sequenceiq.cloudbreak.service.DuplicateKeyValueException;

import ch.qos.logback.classic.Level;

@Component
public class DuplicatedKeyValueExceptionMapper extends BaseExceptionMapper<DuplicateKeyValueException> {

    @Override
    protected Level getLogLevel() {
        return INFO;
    }

    @Override
    public Status getResponseStatus(DuplicateKeyValueException exception) {
        return Status.CONFLICT;
    }

    @Override
    public Class<DuplicateKeyValueException> getExceptionType() {
        return DuplicateKeyValueException.class;
    }

    public static String errorMessage(DuplicateKeyValueException exception) {
        return String.format("The %s name '%s' is already taken, please choose a different one",
                exception.getResourceType().toString().toLowerCase(),
                exception.getValue());
    }
}
