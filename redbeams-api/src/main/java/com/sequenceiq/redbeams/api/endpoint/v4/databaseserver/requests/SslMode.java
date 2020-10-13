package com.sequenceiq.redbeams.api.endpoint.v4.databaseserver.requests;

public enum SslMode {
    ENABLED,
    DISABLED;

    public static boolean isEnabled(SslMode mode) {
        return ENABLED.equals(mode);
    }
}
