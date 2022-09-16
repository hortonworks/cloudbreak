package com.sequenceiq.node.health.client;

import java.util.OptionalInt;

public class CdpNodeStatusMonitorClientException extends Exception {

    private final OptionalInt statusCode;

    private final Boolean nginxResponseHeaderExists;

    public CdpNodeStatusMonitorClientException(String message, boolean nginxResponseHeaderExists) {
        super(message);
        this.statusCode = OptionalInt.empty();
        this.nginxResponseHeaderExists = nginxResponseHeaderExists;
    }

    public CdpNodeStatusMonitorClientException(String message, int statusCode, boolean nginxResponseHeaderExists) {
        super(message);
        this.statusCode = OptionalInt.of(statusCode);
        this.nginxResponseHeaderExists = nginxResponseHeaderExists;
    }

    public CdpNodeStatusMonitorClientException(String message, Throwable cause) {
        super(message, cause);
        statusCode = OptionalInt.empty();
        this.nginxResponseHeaderExists = null;
    }

    public CdpNodeStatusMonitorClientException(String message, Throwable cause, OptionalInt statusCode) {
        super(message, cause);
        this.statusCode = statusCode;
        this.nginxResponseHeaderExists = null;
    }

    public OptionalInt getStatusCode() {
        return statusCode;
    }

    public Boolean getNginxResponseHeaderExists() {
        return nginxResponseHeaderExists;
    }
}
