package com.sequenceiq.cloudbreak.cloud.yarn.client.exception;

public class YarnClientException extends Exception {

    public YarnClientException(String message) {
        super(message);
    }

    public YarnClientException(String message, Throwable cause) {
        super(message, cause);
    }

    public YarnClientException(Throwable cause) {
        super(cause);
    }

    public YarnClientException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }
}
