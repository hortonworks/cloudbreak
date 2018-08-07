package com.sequenceiq.periscope.monitor;

public final class MonitorUpdateRate {

    /**
     * Every 2 minutes.
     */
    public static final String APP_REPORT_UPDATE_RATE_CRON = "0 0/2 * * * ?";

    /**
     * Every 20 seconds.
     */
    public static final String YARN_UPDATE_RATE_CRON = "0/20 * * * * ?";

    /**
     * Every 30 seconds.
     */
    public static final String METRIC_UPDATE_RATE_CRON = "0/30 * * * * ?";

    /**
     * Every 10 seconds.
     */
    public static final String PROMETHEUS_UPDATE_RATE_CRON = "0/10 * * * * ?";

    /**
     * Every 10 seconds.
     */
    public static final String TIME_UPDATE_RATE_CRON = "0/10 * * * * ?";

    /**
     * Every minutes.
     */
    public static final String EVERY_MIN_RATE_CRON = "0 * * * * ?";

    /**
     * Time update rate in ms, aligned to the cron expression.
     */
    public static final long CLUSTER_UPDATE_RATE = 10_000L;

    private MonitorUpdateRate() {
        throw new IllegalStateException();
    }
}
