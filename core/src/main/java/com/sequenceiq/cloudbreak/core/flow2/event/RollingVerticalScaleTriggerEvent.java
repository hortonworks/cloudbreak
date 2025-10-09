package com.sequenceiq.cloudbreak.core.flow2.event;

import java.util.List;
import java.util.StringJoiner;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.StackVerticalScaleV4Request;
import com.sequenceiq.cloudbreak.common.event.AcceptResult;
import com.sequenceiq.cloudbreak.common.json.JsonIgnoreDeserialization;
import com.sequenceiq.cloudbreak.eventbus.Promise;
import com.sequenceiq.cloudbreak.reactor.api.event.StackEvent;

public class RollingVerticalScaleTriggerEvent extends StackEvent {

    private final List<String> instanceIds;

    private final StackVerticalScaleV4Request stackVerticalScaleV4Request;

    public RollingVerticalScaleTriggerEvent(String selector, Long stackId, List<String> instanceIds, StackVerticalScaleV4Request stackVerticalScaleV4Request) {
        super(selector, stackId);
        this.instanceIds = instanceIds;
        this.stackVerticalScaleV4Request = stackVerticalScaleV4Request;
    }

    @JsonCreator
    public RollingVerticalScaleTriggerEvent(
            @JsonProperty("selector") String selector,
            @JsonProperty("resourceId") Long resourceId,
            @JsonProperty("instanceIds") List<String> instanceIds,
            @JsonProperty("stackVerticalScaleV4Request") StackVerticalScaleV4Request stackVerticalScaleV4Request,
            @JsonIgnoreDeserialization @JsonProperty("accepted") Promise<AcceptResult> accepted) {
        super(selector, resourceId, accepted);
        this.instanceIds = instanceIds;
        this.stackVerticalScaleV4Request = stackVerticalScaleV4Request;
    }

    public List<String> getInstanceIds() {
        return instanceIds;
    }

    public StackVerticalScaleV4Request getStackVerticalScaleV4Request() {
        return stackVerticalScaleV4Request;
    }

    @Override
    public String toString() {
        return new StringJoiner(", ", RollingVerticalScaleTriggerEvent.class.getSimpleName() + "[", "]")
                .add("instanceIds=" + instanceIds)
                .add("stackVerticalScaleV4Request=" + stackVerticalScaleV4Request)
                .add(super.toString())
                .toString();
    }
}
