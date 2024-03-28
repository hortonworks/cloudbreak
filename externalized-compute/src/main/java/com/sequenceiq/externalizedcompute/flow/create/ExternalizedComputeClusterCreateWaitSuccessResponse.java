package com.sequenceiq.externalizedcompute.flow.create;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.sequenceiq.externalizedcompute.flow.ExternalizedComputeClusterContext;
import com.sequenceiq.externalizedcompute.flow.ExternalizedComputeClusterEvent;
import com.sequenceiq.flow.event.EventSelectorUtil;

public class ExternalizedComputeClusterCreateWaitSuccessResponse extends ExternalizedComputeClusterEvent {

    @JsonCreator
    public ExternalizedComputeClusterCreateWaitSuccessResponse(
            @JsonProperty("resourceId") Long externalizedComputeClusterId,
            @JsonProperty("actorCrn") String actorCrn) {
        super(externalizedComputeClusterId, actorCrn);
    }

    public ExternalizedComputeClusterCreateWaitSuccessResponse(ExternalizedComputeClusterContext context) {
        super(context);
    }

    @Override
    public String selector() {
        return EventSelectorUtil.selector(ExternalizedComputeClusterCreateWaitSuccessResponse.class);
    }

}
