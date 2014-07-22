package com.sequenceiq.periscope.monitor;

public final class MonitorUpdateRate {

    public static final int APP_REPORT_UPDATE_RATE = 5000;
    public static final int CLUSTER_UPDATE_RATE = 10000;

    private MonitorUpdateRate() {
        throw new IllegalStateException();
    }
}
