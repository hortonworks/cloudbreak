package com.sequenceiq.environment;

public class GetCloudParameterException extends RuntimeException {

    public GetCloudParameterException(String message) {
        super(message);
    }

    public GetCloudParameterException(String message, Throwable cause) {
        super(message, cause);
    }

    public GetCloudParameterException(Throwable cause) {
        super(cause);
    }
}
