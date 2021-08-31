package com.sequenceiq.flow.component.sleep.event;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Objects;

public class SleepConfig {

    private final Duration sleepTime;

    private final LocalDateTime failUntil;

    public SleepConfig(Duration sleepTime, LocalDateTime failUntil) {
        this.sleepTime = sleepTime;
        this.failUntil = failUntil;
    }

    public Duration getSleepTime() {
        return sleepTime;
    }

    public LocalDateTime getFailUntil() {
        return failUntil;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || !getClass().equals(o.getClass())) {
            return false;
        }
        SleepConfig that = (SleepConfig) o;
        return Objects.equals(sleepTime, that.sleepTime) && Objects.equals(failUntil, that.failUntil);
    }

    @Override
    public int hashCode() {
        return Objects.hash(sleepTime, failUntil);
    }
}
