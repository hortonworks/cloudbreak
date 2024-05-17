package com.sequenceiq.environment.environment.flow.externalizedcluster.create.event;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonPOJOBuilder;
import com.sequenceiq.cloudbreak.common.event.AcceptResult;
import com.sequenceiq.cloudbreak.eventbus.Promise;
import com.sequenceiq.environment.environment.flow.creation.event.EnvCreationEvent;
import com.sequenceiq.flow.reactor.api.event.BaseFlowEvent;
import com.sequenceiq.flow.reactor.api.event.BaseNamedFlowEvent;

@JsonDeserialize(builder = ExternalizedComputeClusterCreationEvent.Builder.class)
public class ExternalizedComputeClusterCreationEvent extends BaseNamedFlowEvent {

    public ExternalizedComputeClusterCreationEvent(String selector, Long resourceId, String resourceName, String resourceCrn) {
        super(selector, resourceId, resourceName, resourceCrn);
    }

    public ExternalizedComputeClusterCreationEvent(String selector, Long resourceId, Promise<AcceptResult> accepted, String resourceName, String resourceCrn) {
        super(selector, resourceId, accepted, resourceName, resourceCrn);
    }

    @Override
    public boolean equalsEvent(BaseFlowEvent other) {
        return isClassAndEqualsEvent(EnvCreationEvent.class, other);
    }

    public static ExternalizedComputeClusterCreationEvent.Builder builder() {
        return new ExternalizedComputeClusterCreationEvent.Builder();
    }

    @JsonPOJOBuilder
    public static final class Builder {
        private String resourceName;

        private String resourceCrn;

        private String selector;

        private Long resourceId;

        private Promise<AcceptResult> accepted;

        private Builder() {
        }

        public ExternalizedComputeClusterCreationEvent.Builder withResourceName(String resourceName) {
            this.resourceName = resourceName;
            return this;
        }

        public ExternalizedComputeClusterCreationEvent.Builder withSelector(String selector) {
            this.selector = selector;
            return this;
        }

        public ExternalizedComputeClusterCreationEvent.Builder withResourceId(Long resourceId) {
            this.resourceId = resourceId;
            return this;
        }

        public ExternalizedComputeClusterCreationEvent.Builder withResourceCrn(String resourceCrn) {
            this.resourceCrn = resourceCrn;
            return this;
        }

        public ExternalizedComputeClusterCreationEvent.Builder withAccepted(Promise<AcceptResult> accepted) {
            this.accepted = accepted;
            return this;
        }

        public ExternalizedComputeClusterCreationEvent build() {
            return new ExternalizedComputeClusterCreationEvent(selector, resourceId, accepted, resourceName, resourceCrn);
        }
    }

}
