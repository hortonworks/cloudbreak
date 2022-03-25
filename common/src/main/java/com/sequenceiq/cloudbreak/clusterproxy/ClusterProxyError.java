package com.sequenceiq.cloudbreak.clusterproxy;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

public class ClusterProxyError {

    private final String status;

    private final String code;

    private final String message;

    private final boolean retryable;

    @JsonCreator
    public ClusterProxyError(@JsonProperty("status") String status, @JsonProperty("code") String code, @JsonProperty("message") String message,
            @JsonProperty("retryable") boolean retryable) {
        this.status = status;
        this.code = code;
        this.message = message;
        this.retryable = retryable;
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

    public boolean getRetryable() {
        return retryable;
    }

    @Override
    public String toString() {
        return "ClusterProxyError{" +
                "status='" + status + '\'' +
                ", code='" + code + '\'' +
                ", message='" + message + '\'' +
                ", retryable='" + retryable + '\'' +
                '}';
    }
}
