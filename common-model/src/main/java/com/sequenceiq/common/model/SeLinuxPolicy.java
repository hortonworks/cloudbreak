package com.sequenceiq.common.model;

public enum SeLinuxPolicy {
    ENFORCING, PERMISSIVE;

    public static SeLinuxPolicy fromString(String seLinuxPolicy) {
        if (seLinuxPolicy != null && !seLinuxPolicy.isEmpty()) {
            return valueOf(seLinuxPolicy);
        }
        return SeLinuxPolicy.PERMISSIVE;
    }
}
