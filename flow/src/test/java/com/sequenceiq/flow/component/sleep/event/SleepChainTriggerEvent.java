package com.sequenceiq.flow.component.sleep.event;

import java.util.ArrayList;
import java.util.Objects;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.sequenceiq.cloudbreak.common.event.AcceptResult;
import com.sequenceiq.cloudbreak.common.event.IdempotentEvent;
import com.sequenceiq.cloudbreak.common.json.JsonIgnoreDeserialization;
import com.sequenceiq.flow.component.sleep.SleepChainEventFactory;

import reactor.rx.Promise;

public class SleepChainTriggerEvent implements IdempotentEvent<SleepChainTriggerEvent> {

    private final Long resourceId;

    private final ArrayList<SleepConfig> sleepConfigs;

    private final Promise<AcceptResult> accepted;

    public SleepChainTriggerEvent(Long resourceId, ArrayList<SleepConfig> sleepConfigs) {
        this.resourceId = resourceId;
        this.sleepConfigs = sleepConfigs;
        this.accepted = new Promise<>();
    }

    @JsonCreator
    public SleepChainTriggerEvent(
            @JsonProperty("resourceId") Long resourceId,
            @JsonProperty("sleepConfigs") ArrayList<SleepConfig> sleepConfigs,
            @JsonIgnoreDeserialization Promise<AcceptResult> accepted) {

        this.resourceId = resourceId;
        this.sleepConfigs = sleepConfigs;
        this.accepted = accepted;
    }

    @Override
    public Promise<AcceptResult> accepted() {
        return accepted;
    }

    @Override
    public Long getResourceId() {
        return resourceId;
    }

    @Override
    public String selector() {
        return SleepChainEventFactory.SLEEP_CHAIN_TRIGGER_EVENT;
    }

    public ArrayList<SleepConfig> getSleepConfigs() {
        return sleepConfigs;
    }

    @Override
    public boolean equalsEvent(SleepChainTriggerEvent other) {
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
        SleepChainTriggerEvent that = (SleepChainTriggerEvent) o;
        return Objects.equals(resourceId, that.resourceId) && Objects.equals(sleepConfigs, that.sleepConfigs);
    }

    @Override
    public int hashCode() {
        return Objects.hash(resourceId, sleepConfigs);
    }
}
