package com.sequenceiq.flow.component.sleep.event;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Objects;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

public class SleepConfig {

    private final Duration sleepTime;

    private final LocalDateTime failUntil;

    @JsonCreator
    public SleepConfig(
            @JsonProperty("sleepTime") Duration sleepTime,
            @JsonProperty("failUntil") LocalDateTime failUntil) {
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
