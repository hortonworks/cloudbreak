package com.sequenceiq.sdx.api.model;

public enum SdxClusterShape {
    CUSTOM(Boolean.FALSE, "-cus", Boolean.TRUE),
    LIGHT_DUTY(Boolean.FALSE, "-ld", Boolean.TRUE),
    MEDIUM_DUTY_HA(Boolean.TRUE, "-md", Boolean.TRUE),
    ENTERPRISE(Boolean.TRUE, "-ent", Boolean.TRUE),
    MICRO_DUTY(Boolean.FALSE, "-mic", Boolean.TRUE),
    CONTAINERIZED(Boolean.FALSE, "-con", Boolean.FALSE);

    private final boolean multiAzEnabledByDefault;

    private final String resizeSuffix;

    private final boolean dbConfigSupported;

    SdxClusterShape(Boolean multiAzEnabledByDefault, String resizeSuffix, Boolean dbConfigSupported) {
        this.multiAzEnabledByDefault = multiAzEnabledByDefault;
        this.resizeSuffix = resizeSuffix;
        this.dbConfigSupported = dbConfigSupported;
    }

    public boolean isMultiAzEnabledByDefault() {
        return multiAzEnabledByDefault;
    }

    public String getResizeSuffix() {
        return resizeSuffix;
    }

    public boolean isDbConfigUnsupported() {
        return !dbConfigSupported;
    }

    public boolean isHA() {
        return MEDIUM_DUTY_HA.equals(this) || ENTERPRISE.equals(this);
    }
}
