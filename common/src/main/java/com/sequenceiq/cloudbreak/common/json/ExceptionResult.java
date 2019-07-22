package com.sequenceiq.cloudbreak.common.json;

public class ExceptionResult {

    private String message;

    public ExceptionResult() {
    }

    public ExceptionResult(String message) {
        this.message = message;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }
}
