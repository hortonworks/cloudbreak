package com.sequenceiq.cloudbreak.idbmms.exception;

import static com.google.common.base.Preconditions.checkNotNull;

import java.util.Map;

import io.grpc.Status;

/**
 * Defines the error status of an IDBroker Mapping Management Service (IDBMMS) operation. Based on the {@code io.grpc.Status.Code} of the underlying GRPC call.
 */
public enum IdbmmsOperationErrorStatus {

    NOT_FOUND,
    /**
     * Other error. This is a wildcard for all GRPC errors not covered by other constants above.
     */
    OTHER,
    /**
     * Error status not available. This is a placeholder for all errors where the exact GRPC status was not specified.
     */
    UNSPECIFIED;

    private static final Map<Status.Code, IdbmmsOperationErrorStatus> STATUS_CODE_TO_ERROR_STATUS_MAP = Map.ofEntries(
            Map.entry(Status.Code.NOT_FOUND, NOT_FOUND)
    );

    /**
     * Extracts the error status from the provided {@link Throwable}.
     *
     * @param t {@link Throwable} instance to extract status from; must not be {@code null}
     * @return error status; never {@code null}; never equals {@link #UNSPECIFIED}
     * @throws NullPointerException if {@code t} is {@code null}
     */
    public static IdbmmsOperationErrorStatus fromThrowable(Throwable t) {
        checkNotNull(t);
        return STATUS_CODE_TO_ERROR_STATUS_MAP.getOrDefault(Status.fromThrowable(t).getCode(), OTHER);
    }

}
