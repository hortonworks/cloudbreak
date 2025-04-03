package com.sequenceiq.flow.component.sleep.event;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Objects;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.sequenceiq.cloudbreak.common.event.AcceptResult;
import com.sequenceiq.cloudbreak.common.event.IdempotentEvent;
import com.sequenceiq.cloudbreak.common.json.JsonIgnoreDeserialization;
import com.sequenceiq.cloudbreak.eventbus.Promise;

public class SleepStartEvent implements IdempotentEvent<SleepStartEvent> {

    public static final LocalDateTime NEVER_FAIL = LocalDateTime.MIN;

    public static final LocalDateTime ALWAYS_FAIL = LocalDateTime.MAX;

    private final Long resourceId;

    private final Promise<AcceptResult> accepted;

    private final Duration sleepDuration;

    private final LocalDateTime failUntil;

    public SleepStartEvent(Long resourceId, Duration sleepDuration, LocalDateTime failUntil) {
        this(resourceId, sleepDuration, failUntil, new Promise<>());
    }

    @JsonCreator
    public SleepStartEvent(
            @JsonProperty("resourceId") Long resourceId,
            @JsonProperty("sleepDuration") Duration sleepDuration,
            @JsonProperty("failUntil") LocalDateTime failUntil,
            @JsonIgnoreDeserialization @JsonProperty("accepted") Promise<AcceptResult> accepted) {
        this.resourceId = resourceId;
        this.sleepDuration = sleepDuration;
        this.failUntil = failUntil;
        this.accepted = accepted;
    }

    public static SleepStartEvent neverFail(Long resourceId, Duration sleepDuration) {
        return new SleepStartEvent(resourceId, sleepDuration, NEVER_FAIL);
    }

    public static SleepStartEvent alwaysFail(Long resourceId, Duration sleepDuration) {
        return new SleepStartEvent(resourceId, sleepDuration, ALWAYS_FAIL);
    }

    @Override
    public Promise<AcceptResult> accepted() {
        return accepted;
    }

    @Override
    public Long getResourceId() {
        return resourceId;
    }

    public Duration getSleepDuration() {
        return sleepDuration;
    }

    public LocalDateTime getFailUntil() {
        return failUntil;
    }

    @Override
    public String selector() {
        return SleepEvent.SLEEP_STARTED_EVENT.selector();
    }

    @Override
    public boolean equalsEvent(SleepStartEvent other) {
        return equals(other);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || !getClass().equals(o.getClass())) {
            return false;
        }
        SleepStartEvent that = (SleepStartEvent) o;
        return Objects.equals(resourceId, that.resourceId) && Objects.equals(sleepDuration, that.sleepDuration) && Objects.equals(failUntil, that.failUntil);
    }

    @Override
    public int hashCode() {
        return Objects.hash(resourceId, sleepDuration, failUntil);
    }

    @Override
    public String toString() {
        return "SleepStartEvent{" +
                "resourceId=" + resourceId +
                ", sleepDuration=" + sleepDuration +
                ", failUntil=" + failUntil +
                '}';
    }
}
