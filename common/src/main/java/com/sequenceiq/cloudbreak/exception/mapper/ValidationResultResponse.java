package com.sequenceiq.cloudbreak.exception.mapper;

public class ValidationResultResponse {

    private final String field;

    private final String result;

    public ValidationResultResponse(String field, String result) {
        this.field = field;
        this.result = result;
    }

    public String getResult() {
        return result;
    }

    public String getField() {
        return field;
    }
}
