package com.sequenceiq.cloudbreak.core;

public class CloudbreakImageCatalogException extends Exception {
    public CloudbreakImageCatalogException(String message) {
        super(message);
    }

    public CloudbreakImageCatalogException(String message, Throwable cause) {
        super(message, cause);
    }

    public CloudbreakImageCatalogException(Throwable cause) {
        super(cause);
    }
}
