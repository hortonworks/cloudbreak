package com.sequenceiq.externalizedcompute.flow.delete;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.sequenceiq.flow.event.EventSelectorUtil;

public class ExternalizedComputeClusterAuxiliaryDeleteWaitRequest extends ExternalizedComputeClusterDeleteEvent {

    @JsonCreator
    public ExternalizedComputeClusterAuxiliaryDeleteWaitRequest(
            @JsonProperty("resourceId") Long externalizedComputeClusterId,
            @JsonProperty("actorCrn") String actorCrn,
            @JsonProperty("force") boolean force,
            @JsonProperty("preserveCluster") boolean preserveCluster) {
        super(externalizedComputeClusterId, actorCrn, force, preserveCluster);
    }

    @Override
    public String selector() {
        return EventSelectorUtil.selector(ExternalizedComputeClusterAuxiliaryDeleteWaitRequest.class);
    }
}
