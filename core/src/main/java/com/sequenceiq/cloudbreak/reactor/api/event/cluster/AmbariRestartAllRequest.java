package com.sequenceiq.cloudbreak.reactor.api.event.cluster;

import java.util.Set;
import java.util.StringJoiner;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.sequenceiq.cloudbreak.common.event.FlowPayload;
import com.sequenceiq.cloudbreak.reactor.api.event.resource.AbstractClusterScaleRequest;

public class AmbariRestartAllRequest extends AbstractClusterScaleRequest implements FlowPayload {

    private final boolean rollingRestartEnabled;

    @JsonCreator
    public AmbariRestartAllRequest(
            @JsonProperty("stackId") Long stackId,
            @JsonProperty("hostGroupNames") Set<String> hostGroups,
            @JsonProperty("rollingRestartEnabled") boolean rollingRestartEnabled) {
        super(stackId, hostGroups);
        this.rollingRestartEnabled = rollingRestartEnabled;
    }

    public boolean isRollingRestartEnabled() {
        return rollingRestartEnabled;
    }

    @Override
    public String toString() {
        return new StringJoiner(", ", AmbariRestartAllRequest.class.getSimpleName() + "[", "]")
                .add("rollingRestartEnabled=" + rollingRestartEnabled)
                .add(super.toString())
                .toString();
    }
}
