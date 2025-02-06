package com.sequenceiq.cloudbreak.core.flow2.cluster.restart;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.sequenceiq.cloudbreak.reactor.api.event.StackEvent;

public class RestartInstancesWithRdsStartEvent extends StackEvent {

    private final List<String> instanceIds;

    private final boolean rdsRestartRequired;

    @JsonCreator
    public RestartInstancesWithRdsStartEvent(@JsonProperty("resourceId")Long stackId,
            @JsonProperty("instanceIds")List<String> instanceIds,
            @JsonProperty("rdsRestartRequired") boolean rdsRestartRequired) {
        super(stackId);
        this.instanceIds = instanceIds;
        this.rdsRestartRequired = rdsRestartRequired;
    }

    public List<String> getInstanceIds() {
        return instanceIds;
    }

    public boolean isRdsRestartRequired() {
        return rdsRestartRequired;
    }

}
