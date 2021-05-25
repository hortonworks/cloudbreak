package com.sequenceiq.cdp.databus.model.exception;

public class DatabusRecordProcessingException extends Exception {

    public DatabusRecordProcessingException(String message) {
        super(message);
    }

    public DatabusRecordProcessingException(Throwable cause) {
        super(cause);
    }

    public DatabusRecordProcessingException(String message, Throwable cause) {
        super(message, cause);
    }
}
