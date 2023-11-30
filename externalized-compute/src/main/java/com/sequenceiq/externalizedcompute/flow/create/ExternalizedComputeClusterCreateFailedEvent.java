package com.sequenceiq.externalizedcompute.flow.create;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.sequenceiq.externalizedcompute.flow.ExternalizedComputeClusterEvent;
import com.sequenceiq.externalizedcompute.flow.ExternalizedComputeClusterFailedEvent;

public class ExternalizedComputeClusterCreateFailedEvent extends ExternalizedComputeClusterFailedEvent {

        @JsonCreator
        public ExternalizedComputeClusterCreateFailedEvent(
                @JsonProperty("resourceId") Long externalizedComputeClusterId,
                @JsonProperty("userId") String userId,
                @JsonProperty("exception") Exception exception) {
            super(externalizedComputeClusterId, userId, exception);
        }

        public static ExternalizedComputeClusterCreateFailedEvent from(ExternalizedComputeClusterEvent event, Exception exception) {
            return new ExternalizedComputeClusterCreateFailedEvent(event.getResourceId(), event.getUserId(), exception);
        }

        @Override
        public String selector() {
            return getClass().getSimpleName();
        }

}
