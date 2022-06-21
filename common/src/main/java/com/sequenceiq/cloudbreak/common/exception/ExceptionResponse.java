package com.sequenceiq.cloudbreak.common.exception;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

public class ExceptionResponse {

    private final String message;

    private final  Object payload;

    public ExceptionResponse(String message) {
        this.message = message;
        this.payload = null;
    }

    @JsonCreator(mode = JsonCreator.Mode.PROPERTIES)
    public ExceptionResponse(@JsonProperty("message") String message, @JsonProperty("payload") Object payload) {
        this.message = message;
        this.payload = payload;
    }

    public String getMessage() {
        return message;
    }

    public Object getPayload() {
        return payload;
    }
}
