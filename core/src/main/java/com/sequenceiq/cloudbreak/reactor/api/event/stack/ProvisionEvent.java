package com.sequenceiq.cloudbreak.reactor.api.event.stack;

import java.util.Objects;

import com.sequenceiq.cloudbreak.common.event.AcceptResult;
import com.sequenceiq.cloudbreak.reactor.api.event.StackEvent;

import reactor.rx.Promise;

public class ProvisionEvent extends StackEvent {

    private final ProvisionType provisionType;

    public ProvisionEvent(String selector, Long stackId, ProvisionType provisionType) {
        super(selector, stackId, new Promise<>());
        this.provisionType = provisionType;
    }

    public ProvisionEvent(String selector, Long stackId, ProvisionType provisionType, Promise<AcceptResult> accepted) {
        super(selector, stackId, accepted);
        this.provisionType = provisionType;
    }

    public ProvisionType getProvisionType() {
        return Objects.nonNull(provisionType) ? provisionType : ProvisionType.REGULAR;
    }

    @Override
    public String toString() {
        return "ProvisionEvent{" +
                "provisionType=" + provisionType +
                "} " + super.toString();
    }
}
