package com.sequenceiq.environment.environment.flow.externalizedcluster.reinitialization.event;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonPOJOBuilder;
import com.sequenceiq.cloudbreak.common.event.AcceptResult;
import com.sequenceiq.cloudbreak.eventbus.Promise;
import com.sequenceiq.environment.environment.flow.creation.event.EnvCreationEvent;
import com.sequenceiq.flow.reactor.api.event.BaseFlowEvent;
import com.sequenceiq.flow.reactor.api.event.BaseNamedFlowEvent;

@JsonDeserialize(builder = ExternalizedComputeClusterReInitializationEvent.Builder.class)
public class ExternalizedComputeClusterReInitializationEvent extends BaseNamedFlowEvent {

    private final boolean force;

    public ExternalizedComputeClusterReInitializationEvent(String selector, Long resourceId, String resourceName, String resourceCrn, boolean force) {
        super(selector, resourceId, resourceName, resourceCrn);
        this.force = force;
    }

    public ExternalizedComputeClusterReInitializationEvent(String selector, Long resourceId, Promise<AcceptResult> accepted, String resourceName,
            String resourceCrn, boolean force) {
        super(selector, resourceId, accepted, resourceName, resourceCrn);
        this.force = force;
    }

    @Override
    public boolean equalsEvent(BaseFlowEvent other) {
        return isClassAndEqualsEvent(EnvCreationEvent.class, other);
    }

    public static Builder builder() {
        return new Builder();
    }

    public boolean isForce() {
        return force;
    }

    @JsonPOJOBuilder
    public static final class Builder {

        private String resourceName;

        private String resourceCrn;

        private boolean force;

        private String selector;

        private Long resourceId;

        private Promise<AcceptResult> accepted;

        private Builder() {
        }

        public Builder withResourceName(String resourceName) {
            this.resourceName = resourceName;
            return this;
        }

        public Builder withSelector(String selector) {
            this.selector = selector;
            return this;
        }

        public Builder withResourceId(Long resourceId) {
            this.resourceId = resourceId;
            return this;
        }

        public Builder withResourceCrn(String resourceCrn) {
            this.resourceCrn = resourceCrn;
            return this;
        }

        public Builder withForce(boolean force) {
            this.force = force;
            return this;
        }

        public Builder withAccepted(Promise<AcceptResult> accepted) {
            this.accepted = accepted;
            return this;
        }

        public ExternalizedComputeClusterReInitializationEvent build() {
            return new ExternalizedComputeClusterReInitializationEvent(selector, resourceId, accepted, resourceName, resourceCrn, force);
        }
    }

}
