package com.sequenceiq.cloudbreak.reactor.api.event.stack;

import java.util.Objects;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.sequenceiq.cloudbreak.common.event.AcceptResult;
import com.sequenceiq.cloudbreak.common.json.JsonIgnoreDeserialization;
import com.sequenceiq.cloudbreak.eventbus.Promise;
import com.sequenceiq.cloudbreak.reactor.api.event.StackEvent;

public class ProvisionEvent extends StackEvent {

    private final ProvisionType provisionType;

    public ProvisionEvent(String selector, Long stackId, ProvisionType provisionType) {
        super(selector, stackId, new Promise<>());
        this.provisionType = provisionType;
    }

    @JsonCreator
    public ProvisionEvent(
            @JsonProperty("selector") String selector,
            @JsonProperty("resourceId") Long stackId,
            @JsonProperty("provisionType") ProvisionType provisionType,
            @JsonIgnoreDeserialization @JsonProperty("accepted") Promise<AcceptResult> accepted) {
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
