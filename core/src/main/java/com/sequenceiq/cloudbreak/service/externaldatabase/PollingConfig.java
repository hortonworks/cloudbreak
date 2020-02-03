package com.sequenceiq.cloudbreak.service.externaldatabase;

import java.util.concurrent.TimeUnit;

public class PollingConfig {

    private final long sleepTime;

    private final TimeUnit sleepTimeUnit;

    private final long timeout;

    private final TimeUnit timeoutTimeUnit;

    private final boolean stopPollingIfExceptionOccured;

    private PollingConfig(Builder builder) {
        sleepTime = builder.sleepTime;
        sleepTimeUnit = builder.sleepTimeUnit;
        timeout = builder.timeout;
        timeoutTimeUnit = builder.timeoutTimeUnit;
        stopPollingIfExceptionOccured = builder.stopPollingIfExceptionOccured;
    }

    public long getSleepTime() {
        return sleepTime;
    }

    public long getTimeout() {
        return timeout;
    }

    public TimeUnit getSleepTimeUnit() {
        return sleepTimeUnit;
    }

    public TimeUnit getTimeoutTimeUnit() {
        return timeoutTimeUnit;
    }

    public boolean getStopPollingIfExceptionOccured() {
        return stopPollingIfExceptionOccured;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {

        private static final int DEFAULT_SLEEP = 15;

        private long sleepTime = DEFAULT_SLEEP;

        private TimeUnit sleepTimeUnit = TimeUnit.SECONDS;

        private long timeout = 1;

        private TimeUnit timeoutTimeUnit = TimeUnit.HOURS;

        private boolean stopPollingIfExceptionOccured;

        public Builder withSleepTime(long sleepTime) {
            this.sleepTime = sleepTime;
            return this;
        }

        public Builder withTimeout(long timeout) {
            this.timeout = timeout;
            return this;
        }

        public Builder withSleepTimeUnit(TimeUnit sleepTimeUnit) {
            this.sleepTimeUnit = sleepTimeUnit;
            return this;
        }

        public Builder withTimeoutTimeUnit(TimeUnit timeoutTimeUnit) {
            this.timeoutTimeUnit = timeoutTimeUnit;
            return this;
        }

        public Builder withStopPollingIfExceptionOccured(boolean stopPollingIfExceptionOccured) {
            this.stopPollingIfExceptionOccured = stopPollingIfExceptionOccured;
            return this;
        }

        public PollingConfig build() {
            return new PollingConfig(this);
        }
    }
}
