package com.sequenceiq.node.health.client;

import java.util.OptionalInt;

public class CdpNodeStatusMonitorClientException extends Exception {

    private final OptionalInt statusCode;

    public CdpNodeStatusMonitorClientException(String message) {
        super(message);
        statusCode = OptionalInt.empty();
    }

    public CdpNodeStatusMonitorClientException(String message, int statusCode) {
        super(message);
        this.statusCode = OptionalInt.of(statusCode);
    }

    public CdpNodeStatusMonitorClientException(String message, Throwable cause) {
        super(message, cause);
        statusCode = OptionalInt.empty();
    }

    public CdpNodeStatusMonitorClientException(String message, Throwable cause, OptionalInt statusCode) {
        super(message, cause);
        this.statusCode = statusCode;
    }

    public OptionalInt getStatusCode() {
        return statusCode;
    }
}
