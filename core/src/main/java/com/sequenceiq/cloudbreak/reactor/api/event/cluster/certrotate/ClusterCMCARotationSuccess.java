package com.sequenceiq.cloudbreak.reactor.api.event.cluster.certrotate;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.sequenceiq.cloudbreak.reactor.api.event.StackEvent;

public class ClusterCMCARotationSuccess extends StackEvent {
    @JsonCreator
    public ClusterCMCARotationSuccess(
            @JsonProperty("resourceId") Long stackId) {
        super(stackId);
    }
}
