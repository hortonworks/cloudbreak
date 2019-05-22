package com.sequenceiq.cloudbreak.auth.altus;

public class CrnParseException extends RuntimeException {
    /**
     * Creates a new exception.
     */
    public CrnParseException() {
    }

    /**
     * Creates a new exception with the given message.
     *
     * @param msg message
     */
    public CrnParseException(String msg) {
        super(msg);
    }

    /**
     * Creates a new exception with the given cause.
     *
     * @param cause cause
     */
    public CrnParseException(Throwable cause) {
        super(cause);
    }

    /**
     * Creates a new exception with the given message and cause.
     *
     * @param msg message
     * @param cause cause
     */
    public CrnParseException(String msg, Throwable cause) {
        super(msg, cause);
    }
}
