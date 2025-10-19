package com.sequenceiq.remoteenvironment.exception;

import org.apache.http.HttpStatus;

import com.cloudera.api.swagger.client.ApiException;
import com.sequenceiq.remoteenvironment.RemoteEnvironmentException;

public class OnPremCMApiException extends RemoteEnvironmentException {
    private final int statusCode;

    public OnPremCMApiException(String message) {
        super(message);
        statusCode = HttpStatus.SC_INTERNAL_SERVER_ERROR;
    }

    public OnPremCMApiException(String message, ApiException cause) {
        super(message, cause);
        statusCode = cause.getCode();
    }

    public OnPremCMApiException(String message, Throwable cause, int statusCode) {
        super(message, cause);
        this.statusCode = statusCode;
    }

    public int getStatusCode() {
        return statusCode;
    }
}
