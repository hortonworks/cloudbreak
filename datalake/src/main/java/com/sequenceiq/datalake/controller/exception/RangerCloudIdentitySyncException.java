package com.sequenceiq.datalake.controller.exception;

public class RangerCloudIdentitySyncException extends RuntimeException {

    public RangerCloudIdentitySyncException(String errorMessage) {
        super(errorMessage);
    }

    public RangerCloudIdentitySyncException(String errorMessage, Throwable err) {
        super(errorMessage, err);
    }
}
