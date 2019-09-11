package com.sequenceiq.cloudbreak.idbmms.exception;

/**
 * A {@link RuntimeException} representing a problem encountered during an IDBroker Mapping Management Service (IDBMMS) operation.
 */
public class IdbmmsOperationException extends RuntimeException {

    private final IdbmmsOperationErrorStatus errorStatus;

    /**
     * Constructs a new {@code IdbmmsOperationException} instance with the provided message. The error status will be initialized as
     * {@link IdbmmsOperationErrorStatus#UNSPECIFIED}.
     *
     * @param message the detail message
     */
    public IdbmmsOperationException(String message) {
        super(message);
        errorStatus = IdbmmsOperationErrorStatus.UNSPECIFIED;
    }

    /**
     * Constructs a new {@code IdbmmsOperationException} instance with the provided message and cause. The error status will be initialized as
     * {@code IdbmmsOperationErrorStatus.fromThrowable(cause)}, or {@link IdbmmsOperationErrorStatus#UNSPECIFIED} if {@code cause} is {@code null}.
     *
     * @param message the detail message
     * @param cause the cause
     */
    public IdbmmsOperationException(String message, Throwable cause) {
        super(message, cause);
        errorStatus = cause == null ? IdbmmsOperationErrorStatus.UNSPECIFIED : IdbmmsOperationErrorStatus.fromThrowable(cause);
    }

    /**
     * Retrieves the error status of {@code this} instance.
     *
     * @return error status; never {@code null}
     */
    public IdbmmsOperationErrorStatus getErrorStatus() {
        return errorStatus;
    }

}
