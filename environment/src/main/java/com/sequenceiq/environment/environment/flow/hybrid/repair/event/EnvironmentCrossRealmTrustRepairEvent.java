package com.sequenceiq.environment.environment.flow.hybrid.repair.event;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonPOJOBuilder;
import com.sequenceiq.cloudbreak.common.event.AcceptResult;
import com.sequenceiq.cloudbreak.eventbus.Promise;
import com.sequenceiq.flow.reactor.api.event.BaseNamedFlowEvent;

@JsonDeserialize(builder = EnvironmentCrossRealmTrustRepairEvent.Builder.class)
public class EnvironmentCrossRealmTrustRepairEvent extends BaseNamedFlowEvent {

    public EnvironmentCrossRealmTrustRepairEvent(
            String selector,
            Long resourceId,
            Promise<AcceptResult> accepted,
            String resourceName,
            String resourceCrn) {
        super(selector, resourceId, accepted, resourceName, resourceCrn);
    }

    public static Builder builder() {
        return new Builder();
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

        public Builder withAccepted(Promise<AcceptResult> accepted) {
            this.accepted = accepted;
            return this;
        }

        public Builder withResourceCrn(String resourceCrn) {
            this.resourceCrn = resourceCrn;
            return this;
        }

        public EnvironmentCrossRealmTrustRepairEvent build() {
            return new EnvironmentCrossRealmTrustRepairEvent(
                    selector,
                    resourceId,
                    accepted,
                    resourceName,
                    resourceCrn);
        }
    }
}
