package com.sequenceiq.flow.component.sleep.event;

import java.util.ArrayList;
import java.util.Objects;

import com.sequenceiq.cloudbreak.common.event.AcceptResult;
import com.sequenceiq.cloudbreak.common.event.IdempotentEvent;

import reactor.rx.Promise;

public class NestedSleepChainTriggerEvent implements IdempotentEvent<NestedSleepChainTriggerEvent> {

    public static final String NESTED_SLEEP_CHAIN_TRIGGER_EVENT = "NESTED_SLEEP_CHAIN_TRIGGER_EVENT";

    private final Long resourceId;

    private final ArrayList<SleepChainTriggerEvent> sleepChainTriggerEvents;

    private final Promise<AcceptResult> accepted;

    public NestedSleepChainTriggerEvent(Long resourceId,
            ArrayList<SleepChainTriggerEvent> sleepChainTriggerEvents,
            Promise<AcceptResult> accepted) {
        this.resourceId = resourceId;
        this.sleepChainTriggerEvents = sleepChainTriggerEvents;
        this.accepted = accepted;
    }

    public ArrayList<SleepChainTriggerEvent> getSleepChainTriggerEvents() {
        return sleepChainTriggerEvents;
    }

    @Override
    public Long getResourceId() {
        return resourceId;
    }

    @Override
    public String selector() {
        return NESTED_SLEEP_CHAIN_TRIGGER_EVENT;
    }

    @Override
    public Promise<AcceptResult> accepted() {
        return accepted;
    }

    @Override
    public boolean equalsEvent(NestedSleepChainTriggerEvent other) {
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
        NestedSleepChainTriggerEvent that = (NestedSleepChainTriggerEvent) o;
        return Objects.equals(resourceId, that.resourceId)
                && Objects.equals(sleepChainTriggerEvents, that.sleepChainTriggerEvents);
    }

    @Override
    public int hashCode() {
        return Objects.hash(resourceId, sleepChainTriggerEvents);
    }
}
