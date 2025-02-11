package com.sequenceiq.cloudbreak.core.flow2.event;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.sequenceiq.cloudbreak.common.event.AcceptResult;
import com.sequenceiq.cloudbreak.common.json.JsonIgnoreDeserialization;
import com.sequenceiq.cloudbreak.eventbus.Promise;
import com.sequenceiq.cloudbreak.reactor.api.event.StackEvent;

public class RestartInstancesEvent extends StackEvent {

    private final List<String> instanceIds;

    @JsonCreator
    public RestartInstancesEvent(
            @JsonProperty("selector") String selector,
            @JsonProperty("resourceId") Long resourceId,
            @JsonProperty("instanceIds") List<String> instanceIds,
            @JsonIgnoreDeserialization @JsonProperty("accepted") Promise<AcceptResult> accepted) {
        super(selector, resourceId, accepted);
        this.instanceIds = instanceIds;
    }

    public List<String> getInstanceIds() {
        return instanceIds;
    }

    @Override
    public String toString() {
        return "RestartInstancesEvent{" +
                ", instanceIds=" + instanceIds +
                '}';
    }
}
