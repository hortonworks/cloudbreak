package com.sequenceiq.common.api.type;

import java.util.Locale;

public enum EnvironmentType {

    PUBLIC_CLOUD, HYBRID, HYBRID_BASE;

    public boolean isHybrid() {
        return HYBRID.equals(this);
    }

    public static boolean isHybridFromEnvironmentTypeString(String environmentType) {
        return HYBRID.equals(environmentType(environmentType));
    }

    public static EnvironmentType environmentType(String environmentType) {
        return environmentType == null ? PUBLIC_CLOUD : EnvironmentType.valueOf(environmentType.toUpperCase(Locale.ROOT));
    }
}
