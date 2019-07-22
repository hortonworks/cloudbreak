package com.sequenceiq.freeipa.service.image;

public class ImageCatalogException extends RuntimeException {
    public ImageCatalogException(String message) {
        super(message);
    }

    public ImageCatalogException(String message, Throwable cause) {
        super(message, cause);
    }

    public ImageCatalogException(Throwable cause) {
        super(cause);
    }
}
