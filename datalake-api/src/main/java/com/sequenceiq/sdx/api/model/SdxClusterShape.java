package com.sequenceiq.sdx.api.model;

import com.fasterxml.jackson.annotation.JsonAlias;

public enum SdxClusterShape {
    CUSTOM(Boolean.FALSE, "-cus", Boolean.TRUE),
    LIGHT_DUTY(Boolean.FALSE, "-ld", Boolean.TRUE),
    @JsonAlias("LIGHT_DUTY_WITHOUT_HBASE")
    LIGHT_DUTY_PRO(Boolean.FALSE, "-ldl", Boolean.TRUE),
    MEDIUM_DUTY_HA(Boolean.TRUE, "-md", Boolean.TRUE),
    ENTERPRISE(Boolean.TRUE, "-ent", Boolean.TRUE),
    @JsonAlias("ENTERPRISE_WITHOUT_HBASE")
    ENTERPRISE_PRO(Boolean.TRUE, "-enl", Boolean.TRUE),
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
        return MEDIUM_DUTY_HA.equals(this) || ENTERPRISE.equals(this) || ENTERPRISE_PRO.equals(this);
    }

    public boolean isWithoutHbase() {
        return LIGHT_DUTY_PRO.equals(this) || ENTERPRISE_PRO.equals(this);
    }
}
