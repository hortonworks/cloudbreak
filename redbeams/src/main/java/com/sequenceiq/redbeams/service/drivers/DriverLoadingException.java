package com.sequenceiq.redbeams.service.drivers;

import static java.util.Objects.requireNonNull;

/**
 * Errors that occur when loading a DB driver.
 */
public class DriverLoadingException extends RuntimeException {
    private final String field;

    private final String errorCode;

    public DriverLoadingException(String field, String errorCode, String message) {
        super(message);
        this.field = requireNonNull(field, "field is null");
        this.errorCode = requireNonNull(errorCode, "errorCode is null");
    }

    /**
     * Returns the field.
     *
     * @return the field
     */
    public String getField() {
        return field;
    }

    /**
     * Returns the error code.
     *
     * @return the error code
     */
    public String getErrorCode() {
        return errorCode;
    }
}
