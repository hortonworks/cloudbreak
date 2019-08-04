package com.sequenceiq.cloudbreak.idbmms.exception;

/**
 * A {@link RuntimeException} representing a problem encountered during an IDBroker Mapping Management Service operation.
 */
public class IdbmmsOperationException extends RuntimeException {

    public IdbmmsOperationException(String message) {
        super(message);
    }

    public IdbmmsOperationException(String message, Throwable cause) {
        super(message, cause);
    }

}
