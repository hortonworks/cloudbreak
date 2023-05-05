package com.sequenceiq.environment.exception.mapper;

import javax.annotation.Priority;
import javax.ws.rs.core.Response.Status;

import org.springframework.stereotype.Component;

import com.fasterxml.jackson.core.JsonParseException;

@Component
@Priority(1)
public class JsonParseExceptionMapper extends EnvironmentBaseExceptionMapper<JsonParseException> {

    @Override
    public Status getResponseStatus(JsonParseException exception) {
        return Status.BAD_REQUEST;
    }

    @Override
    public Class<JsonParseException> getExceptionType() {
        return JsonParseException.class;
    }
}
