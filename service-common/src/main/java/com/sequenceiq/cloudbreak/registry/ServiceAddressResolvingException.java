package com.sequenceiq.cloudbreak.registry;

public class ServiceAddressResolvingException extends RuntimeException {

    public ServiceAddressResolvingException(String message) {
        super(message);
    }

    public ServiceAddressResolvingException(String message, Throwable cause) {
        super(message, cause);
    }

    public ServiceAddressResolvingException(Throwable cause) {
        super(cause);
    }

}
