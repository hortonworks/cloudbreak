package com.sequenceiq.externalizedcompute.flow.delete;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.sequenceiq.externalizedcompute.flow.ExternalizedComputeClusterContext;
import com.sequenceiq.externalizedcompute.flow.ExternalizedComputeClusterEvent;
import com.sequenceiq.flow.event.EventSelectorUtil;

public class ExternalizedComputeClusterDeleteWaitSuccessResponse extends ExternalizedComputeClusterEvent {

    @JsonCreator
    public ExternalizedComputeClusterDeleteWaitSuccessResponse(
            @JsonProperty("resourceId") Long externalizedComputeClusterId,
            @JsonProperty("actorCrn") String actorCrn) {
        super(externalizedComputeClusterId, actorCrn);
    }

    public ExternalizedComputeClusterDeleteWaitSuccessResponse(ExternalizedComputeClusterContext context) {
        super(context);
    }

    @Override
    public String selector() {
        return EventSelectorUtil.selector(ExternalizedComputeClusterDeleteWaitSuccessResponse.class);
    }

}
