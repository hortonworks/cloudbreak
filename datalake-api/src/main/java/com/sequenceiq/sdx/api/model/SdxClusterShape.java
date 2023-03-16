package com.sequenceiq.sdx.api.model;

public enum SdxClusterShape {
    CUSTOM(Boolean.FALSE),
    LIGHT_DUTY(Boolean.FALSE),
    MEDIUM_DUTY_HA(Boolean.TRUE),
    MEDIUM_DUTY_HA_SCALABLE(Boolean.TRUE),
    SCALABLE(Boolean.TRUE),
    MICRO_DUTY(Boolean.FALSE);

    private final boolean multiAzEnabledByDefault;

    SdxClusterShape(Boolean multiAzEnabledByDefault) {
        this.multiAzEnabledByDefault = multiAzEnabledByDefault;
    }

    public boolean isMultiAzEnabledByDefault() {
        return multiAzEnabledByDefault;
    }

    public boolean isHA() {
        return MEDIUM_DUTY_HA.equals(this) || SCALABLE.equals(this);
    }
}
