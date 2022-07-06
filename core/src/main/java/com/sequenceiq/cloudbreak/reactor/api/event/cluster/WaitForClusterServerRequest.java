package com.sequenceiq.cloudbreak.reactor.api.event.cluster;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.sequenceiq.cloudbreak.common.event.AcceptResult;
import com.sequenceiq.cloudbreak.common.json.JsonIgnoreDeserialization;
import com.sequenceiq.cloudbreak.reactor.api.event.StackEvent;

import reactor.rx.Promise;

public class WaitForClusterServerRequest extends StackEvent {
    public WaitForClusterServerRequest(Long stackId) {
        super(stackId);
    }

    public WaitForClusterServerRequest(String selector, Long stackId) {
        super(selector, stackId);
    }

    @JsonCreator
    public WaitForClusterServerRequest(
            @JsonProperty("selector") String selector,
            @JsonProperty("resourceId") Long stackId,
            @JsonIgnoreDeserialization Promise<AcceptResult> accepted) {
        super(selector, stackId, accepted);
    }
}
