package com.sequenceiq.cloudbreak.ccm.exception;

/**
 * Exception for CCM failures.
 */
public class CcmException extends Exception {

    /**
     * Whether the exception represents a transient condition.
     */
    private final boolean retryable;

    /**
     * Creates a CCM exception.
     *
     * @param retryable whether the exception represents a transient condition
     */
    public CcmException(boolean retryable) {
        this.retryable = retryable;
    }

    /**
     * Creates a CCM exception with the specified parameters.
     *
     * @param message   the message
     * @param retryable whether the exception represents a transient condition
     */
    public CcmException(String message, boolean retryable) {
        super(message);
        this.retryable = retryable;
    }

    /**
     * Creates a CCM exception with the specified parameters.
     *
     * @param message   the message
     * @param cause     the cause
     * @param retryable whether the exception represents a transient condition
     */
    public CcmException(String message, Throwable cause, boolean retryable) {
        super(message, cause);
        this.retryable = retryable;
    }

    /**
     * Creates a CCM exception with the specified parameters.
     *
     * @param cause     the cause
     * @param retryable whether the exception represents a transient condition
     */
    public CcmException(Throwable cause, boolean retryable) {
        super(cause);
        this.retryable = retryable;
    }

    /**
     * Creates a CCM exception with the specified parameters.
     *
     * @param message            the message
     * @param cause              the cause
     * @param enableSuppression  whether suppression is enabled
     * @param writableStackTrace whether the stack trace should be writable
     * @param retryable          whether the exception represents a transient condition
     */
    public CcmException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace, boolean retryable) {
        super(message, cause, enableSuppression, writableStackTrace);
        this.retryable = retryable;
    }

    /**
     * \Returns whether the exception represents a transient condition.
     *
     * @return whether the exception represents a transient condition
     */
    public boolean isRetryable() {
        return retryable;
    }
}
