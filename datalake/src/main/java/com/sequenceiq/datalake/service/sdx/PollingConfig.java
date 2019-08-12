package com.sequenceiq.datalake.service.sdx;

import java.util.concurrent.TimeUnit;

public class PollingConfig {

    private long sleepTime;

    private long duration;

    private TimeUnit sleepTimeUnit;

    private TimeUnit durationTimeUnit;

    private Boolean stopPollingIfExceptionOccured = Boolean.FALSE;

    public PollingConfig(long sleepTime, TimeUnit sleepTimeUnit, long duration, TimeUnit durationTimeUnit) {
        this.sleepTime = sleepTime;
        this.duration = duration;
        this.sleepTimeUnit = sleepTimeUnit;
        this.durationTimeUnit = durationTimeUnit;
    }

    public long getSleepTime() {
        return sleepTime;
    }

    public long getDuration() {
        return duration;
    }

    public TimeUnit getSleepTimeUnit() {
        return sleepTimeUnit;
    }

    public TimeUnit getDurationTimeUnit() {
        return durationTimeUnit;
    }

    public Boolean getStopPollingIfExceptionOccured() {
        return stopPollingIfExceptionOccured;
    }

    public void setStopPollingIfExceptionOccured(Boolean stopPollingIfExceptionOccured) {
        this.stopPollingIfExceptionOccured = stopPollingIfExceptionOccured;
    }
}
