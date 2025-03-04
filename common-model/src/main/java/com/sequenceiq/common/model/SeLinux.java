package com.sequenceiq.common.model;

import java.util.Locale;

public enum SeLinux {
    ENFORCING, PERMISSIVE, DISABLED;

    public static SeLinux fromStringWithFallback(String seLinux) {
        if (seLinux != null && !seLinux.isEmpty()) {
            return valueOf(seLinux.toUpperCase(Locale.ROOT));
        }
        return SeLinux.PERMISSIVE;
    }
}
