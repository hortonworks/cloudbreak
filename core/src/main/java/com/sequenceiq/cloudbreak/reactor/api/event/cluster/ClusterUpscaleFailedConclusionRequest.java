package com.sequenceiq.cloudbreak.reactor.api.event.cluster;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.sequenceiq.cloudbreak.reactor.api.event.StackEvent;

public class ClusterUpscaleFailedConclusionRequest extends StackEvent {

    private boolean repair;

    @JsonCreator
    public ClusterUpscaleFailedConclusionRequest(
            @JsonProperty("resourceId") Long stackId,
            @JsonProperty("repair") boolean repair) {
        super(stackId);
        this.repair = repair;
    }

    public boolean isRepair() {
        return repair;
    }

    public void setRepair(boolean repair) {
        this.repair = repair;
    }

}
