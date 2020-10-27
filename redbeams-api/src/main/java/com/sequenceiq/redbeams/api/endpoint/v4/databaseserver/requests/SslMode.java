package com.sequenceiq.redbeams.api.endpoint.v4.databaseserver.requests;

/**
 * SSL enforcement mode used by a database server.
 */
public enum SslMode {
    ENABLED,
    DISABLED;

    public static boolean isEnabled(SslMode mode) {
        return ENABLED.equals(mode);
    }
}
