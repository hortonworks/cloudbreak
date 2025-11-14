package com.sequenceiq.cloudbreak.core.flow2.cluster.verticalscale.rollingvs;

public enum RollingVerticalScaleStatus {
    INIT("INIT"),
    STOPPED("STOP"),
    SCALED("SCALED"),
    SUCCESS("SUCCESS"),
    STOP_FAILED("STOP"),
    SCALING_FAILED("VERTICAL_SCALE"),
    RESTART_FAILED("RESTART"),
    SCALING_RESTART_FAILED("VERTICAL_SCALE and RESTART"),

    SERVICES_UNHEALTHY("SERVICES_UNHEALTHY");

    private final String message;

    RollingVerticalScaleStatus(String message) {
        this.message = message;
    }

    public String getMessage() {
        return message;
    }
}
