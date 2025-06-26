package com.sequenceiq.common.api.type;

public enum EnvironmentType {

    PUBLIC_CLOUD, HYBRID, HYBRID_BASE;

    public static boolean isHybrid(String type) {
        return HYBRID.name().equalsIgnoreCase(type);
    }
}
