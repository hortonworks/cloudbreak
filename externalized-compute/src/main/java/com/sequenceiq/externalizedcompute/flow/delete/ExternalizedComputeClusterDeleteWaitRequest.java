package com.sequenceiq.externalizedcompute.flow.delete;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.sequenceiq.externalizedcompute.flow.ExternalizedComputeClusterContext;
import com.sequenceiq.externalizedcompute.flow.ExternalizedComputeClusterEvent;
import com.sequenceiq.flow.event.EventSelectorUtil;

public class ExternalizedComputeClusterDeleteWaitRequest extends ExternalizedComputeClusterEvent {

    private boolean force;

    @JsonCreator
    public ExternalizedComputeClusterDeleteWaitRequest(
            @JsonProperty("resourceId") Long externalizedComputeClusterId,
            @JsonProperty("actorCrn") String actorCrn,
            @JsonProperty("force") boolean force) {
        super(externalizedComputeClusterId, actorCrn);
        this.force = force;
    }

    public ExternalizedComputeClusterDeleteWaitRequest(ExternalizedComputeClusterContext context) {
        super(context);
    }

    @Override
    public String selector() {
        return EventSelectorUtil.selector(ExternalizedComputeClusterDeleteWaitRequest.class);
    }

    public boolean isForce() {
        return force;
    }
}
