package com.sequenceiq.datalake.service.sdx.flowwait;

public class SdxWaitException extends RuntimeException {

    public SdxWaitException(String message, Throwable t) {
        super(message, t);
    }
}
