package com.sequenceiq.cloudbreak.grpc.util;

import java.util.Set;

import com.google.common.collect.Sets;

import io.grpc.Status;

/**
 * Provides GRPC-related utilities.
 */
public class GrpcUtil {

    /**
     * Those statuse codes that indicate a transient problem.
     */
    private static final Set<Status.Code> RETRYABLE_STATUS_CODES =
            Sets.immutableEnumSet(Status.Code.UNAVAILABLE, Status.Code.NOT_FOUND);

    /**
     * Private constructor to prevent instantiation.
     */
    private GrpcUtil() {
    }

    /**
     * Returns whether the specified status code indicates a transient problem.
     *
     * @param statusCode the status code
     * @return whether the specified status code indicates a transient problem
     */
    public static boolean isRetryable(Status.Code statusCode) {
        return RETRYABLE_STATUS_CODES.contains(statusCode);
    }
}
