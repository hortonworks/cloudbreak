package com.sequenceiq.externalizedcompute.flow.delete;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.sequenceiq.externalizedcompute.flow.ExternalizedComputeClusterEvent;
import com.sequenceiq.externalizedcompute.flow.ExternalizedComputeClusterFailedEvent;
import com.sequenceiq.flow.event.EventSelectorUtil;

public class ExternalizedComputeClusterDeleteFailedEvent extends ExternalizedComputeClusterFailedEvent {

        @JsonCreator
        public ExternalizedComputeClusterDeleteFailedEvent(
                @JsonProperty("resourceId") Long externalizedComputeClusterId,
                @JsonProperty("actorCrn") String actorCrn,
                @JsonProperty("exception") Exception exception) {
            super(externalizedComputeClusterId, actorCrn, exception);
        }

        public static ExternalizedComputeClusterDeleteFailedEvent from(ExternalizedComputeClusterEvent event, Exception exception) {
            return new ExternalizedComputeClusterDeleteFailedEvent(event.getResourceId(), event.getActorCrn(), exception);
        }

        @Override
        public String selector() {
            return EventSelectorUtil.selector(ExternalizedComputeClusterDeleteFailedEvent.class);
        }

}
