package com.sequenceiq.datalake.service.sdx;

import java.util.concurrent.TimeUnit;

public class PollingConfig {

    private final long sleepTime;

    private final long duration;

    private final TimeUnit sleepTimeUnit;

    private final TimeUnit durationTimeUnit;

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

    public Boolean getStopPollingIfExceptionOccurred() {
        return stopPollingIfExceptionOccured;
    }

    public PollingConfig withStopPollingIfExceptionOccurred(Boolean stopPollingIfExceptionOccurred) {
        this.stopPollingIfExceptionOccured = stopPollingIfExceptionOccurred;
        return this;
    }

    @Override
    public String toString() {
        return "PollingConfig{" +
                "sleepTime=" + sleepTime +
                ", duration=" + duration +
                ", sleepTimeUnit=" + sleepTimeUnit +
                ", durationTimeUnit=" + durationTimeUnit +
                ", stopPollingIfExceptionOccured=" + stopPollingIfExceptionOccured +
                '}';
    }
}
