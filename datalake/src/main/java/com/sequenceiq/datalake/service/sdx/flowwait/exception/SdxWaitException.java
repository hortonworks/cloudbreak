package com.sequenceiq.datalake.service.sdx.flowwait.exception;

public class SdxWaitException extends RuntimeException {

    public SdxWaitException(String message, Throwable t) {
        super(message, t);
    }
}
