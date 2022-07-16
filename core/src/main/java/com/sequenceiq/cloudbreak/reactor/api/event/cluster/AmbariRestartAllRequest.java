package com.sequenceiq.cloudbreak.reactor.api.event.cluster;

import java.util.Set;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.sequenceiq.cloudbreak.common.event.FlowPayload;
import com.sequenceiq.cloudbreak.reactor.api.event.resource.AbstractClusterScaleRequest;

public class AmbariRestartAllRequest extends AbstractClusterScaleRequest implements FlowPayload {
    @JsonCreator
    public AmbariRestartAllRequest(
            @JsonProperty("stackId") Long stackId,
            @JsonProperty("hostGroupNames") Set<String> hostGroups) {
        super(stackId, hostGroups);
    }
}
