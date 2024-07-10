package com.sequenceiq.sdx.api.model;

public enum SdxClusterShape {
    CUSTOM(Boolean.FALSE, "-cus"),
    LIGHT_DUTY(Boolean.FALSE, "-ld"),
    MEDIUM_DUTY_HA(Boolean.TRUE, "-md"),
    ENTERPRISE(Boolean.TRUE, "-ent"),
    MICRO_DUTY(Boolean.FALSE, "-mic"),
    CONTAINERIZED(Boolean.FALSE, "-con");

    private final boolean multiAzEnabledByDefault;

    private final String resizeSuffix;

    SdxClusterShape(Boolean multiAzEnabledByDefault, String resizeSuffix) {
        this.multiAzEnabledByDefault = multiAzEnabledByDefault;
        this.resizeSuffix = resizeSuffix;
    }

    public boolean isMultiAzEnabledByDefault() {
        return multiAzEnabledByDefault;
    }

    public String getResizeSuffix() {
        return resizeSuffix;
    }

    public boolean isHA() {
        return MEDIUM_DUTY_HA.equals(this) || ENTERPRISE.equals(this);
    }
}
