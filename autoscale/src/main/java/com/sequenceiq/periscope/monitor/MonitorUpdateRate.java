package com.sequenceiq.periscope.monitor;

public final class MonitorUpdateRate {

    /**
     * Every 2 minutes.
     */
    public static final String APP_REPORT_UPDATE_RATE_CRON = "0 0/2 * * * ?";

    /**
     * Every 30 seconds.
     */
    public static final String METRIC_UPDATE_RATE_CRON = "0/30 * * * * ?";

    /**
     * Every 10 seconds.
     */
    public static final String PROMETHEUS_UPDATE_RATE_CRON = "0/10 * * * * ?";

    /**
     * Time update rate in ms, aligned to the cron expression.
     */
    public static final long CRON_UPDATE_RATE_IN_MILLIS = 10_000L;

    /**
     * Every 10 seconds.
     */
    public static final String TIME_UPDATE_RATE_CRON = "0/" + CRON_UPDATE_RATE_IN_MILLIS / 1000 + " * * * * ?";

    /**
     * Every minutes.
     */
    public static final String EVERY_MIN_RATE_CRON = "0 * * * * ?";

    /**
     * Every 2 minutes.
     */
    public static final String EVERY_TWO_MIN_RATE_CRON = "0 0/2 * * * ?";

    /**
     * Every 5 minutes.
     */
    public static final String EVERY_FIVE_MIN_RATE_CRON = "0 0/5 * * * ?";

    /**
     * Every minutes.
     */
    public static final String EVERY_QUARTER_PAST_SEC_RATE_CRON = "15 * * * * ?";

    /**
     * Every minutes.
     */
    public static final String EVERY_QUARTER_TO_SEC_RATE_CRON = "45 * * * * ?";

    private MonitorUpdateRate() {
    }
}
