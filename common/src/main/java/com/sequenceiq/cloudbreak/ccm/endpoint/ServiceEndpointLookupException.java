package com.sequenceiq.cloudbreak.ccm.endpoint;

import com.sequenceiq.cloudbreak.ccm.exception.CcmException;

/**
 * Exception for service endpoint lookup failures.
 */
public class ServiceEndpointLookupException extends CcmException {

    /**
     * Creates a service endpoint lookup exception.
     *
     * @param retryable whether the exception represents a transient condition
     */
    public ServiceEndpointLookupException(boolean retryable) {
        super(retryable);
    }

    /**
     * Creates a service endpoint lookup exception with the specified parameters.
     *
     * @param message   the message
     * @param retryable whether the exception represents a transient condition
     */
    public ServiceEndpointLookupException(String message, boolean retryable) {
        super(message, retryable);
    }

    /**
     * Creates a service endpoint lookup exception with the specified parameters.
     *
     * @param message   the message
     * @param cause     the cause
     * @param retryable whether the exception represents a transient condition
     */
    public ServiceEndpointLookupException(String message, Throwable cause, boolean retryable) {
        super(message, cause, retryable);
    }

    /**
     * Creates a service endpoint lookup exception with the specified parameters.
     *
     * @param cause     the cause
     * @param retryable whether the exception represents a transient condition
     */
    public ServiceEndpointLookupException(Throwable cause, boolean retryable) {
        super(cause, retryable);
    }

    /**
     * Creates a service endpoint lookup exception with the specified parameters.
     *
     * @param message            the message
     * @param cause              the cause
     * @param enableSuppression  whether suppression is enabled
     * @param writableStackTrace whether the stack trace should be writable
     * @param retryable          whether the exception represents a transient condition
     */
    public ServiceEndpointLookupException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace, boolean retryable) {
        super(message, cause, enableSuppression, writableStackTrace, retryable);
    }
}
