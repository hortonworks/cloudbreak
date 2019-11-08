package com.sequenceiq.freeipa.client;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

public class ClusterProxyError {

    private final String status;

    private final String code;

    private final String message;

    @JsonCreator
    public ClusterProxyError(@JsonProperty("status") String status, @JsonProperty("code") String code, @JsonProperty("message") String message) {
        this.status = status;
        this.code = code;
        this.message = message;
    }

    public String getStatus() {
        return status;
    }

    public String getCode() {
        return code;
    }

    public String getMessage() {
        return message;
    }

    @Override
    public String toString() {
        return "ClusterProxyError{" +
                "status='" + status + '\'' +
                ", code='" + code + '\'' +
                ", message='" + message + '\'' +
                '}';
    }
}
