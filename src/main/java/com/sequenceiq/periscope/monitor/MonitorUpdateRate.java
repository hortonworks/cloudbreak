package com.sequenceiq.periscope.monitor;

public final class MonitorUpdateRate {

    public static final int QUEUE_UPDATE_RATE = 30000;
    public static final int APP_REPORT_UPDATE_RATE = 20000;

    private MonitorUpdateRate() {
        throw new IllegalStateException();
    }
}
