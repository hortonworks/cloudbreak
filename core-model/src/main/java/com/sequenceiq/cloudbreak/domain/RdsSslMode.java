package com.sequenceiq.cloudbreak.domain;

public enum RdsSslMode {
    ENABLED,
    DISABLED;

    public static boolean isEnabled(RdsSslMode mode) {
        return ENABLED.equals(mode);
    }

    public static RdsSslMode fromBoolean(boolean sslEnforcementEnabled) {
        return sslEnforcementEnabled ? ENABLED : DISABLED;
    }
}
