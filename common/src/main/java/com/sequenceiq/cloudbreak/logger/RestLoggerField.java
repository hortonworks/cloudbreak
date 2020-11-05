package com.sequenceiq.cloudbreak.logger;

public enum RestLoggerField {

    START_TIME("startTime"),
    END_TIME("endTime"),
    DURATION("duration"),
    HTTP_METHOD("httpMethod"),
    PATH("path"),
    QUERY_STRING("queryString"),
    CLIENT_IP("clientIp"),
    REQUEST("request"),
    RESPONSE_STATUS("responseStatus"),
    RESPONSE("response");

    private String field;

    RestLoggerField(String field) {
        this.field = field;
    }

    public String field() {
        return field;
    }
}