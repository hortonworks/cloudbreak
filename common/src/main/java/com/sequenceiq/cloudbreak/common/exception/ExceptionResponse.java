package com.sequenceiq.cloudbreak.common.exception;

public class ExceptionResponse {

    private final String message;

    private final  Object payload;

    public ExceptionResponse(String message) {
        this.message = message;
        this.payload = null;
    }

    public ExceptionResponse(String message, Object payload) {
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
