package com.sequenceiq.cloudbreak.core;

public class CloudbreakImageFallbackRequiredException extends Exception {
    public CloudbreakImageFallbackRequiredException(String message) {
        super(message);
    }

    public CloudbreakImageFallbackRequiredException(String message, Throwable cause) {
        super(message, cause);
    }

    public CloudbreakImageFallbackRequiredException(Throwable cause) {
        super(cause);
    }
}
