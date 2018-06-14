package com.sequenceiq.it.cloudbreak.exception;

public class ProxyMethodInvocationException extends RuntimeException {

    public ProxyMethodInvocationException(String message) {
        super(message);
    }

    public ProxyMethodInvocationException(String message, Throwable cause) {
        super(message, cause);
    }

    public ProxyMethodInvocationException(Throwable cause) {
        super(cause);
    }

}
