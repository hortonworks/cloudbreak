package com.sequenceiq.environment.environment.flow.verticalscale.freeipa.event;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonPOJOBuilder;
import com.sequenceiq.cloudbreak.common.event.AcceptResult;
import com.sequenceiq.cloudbreak.eventbus.Promise;
import com.sequenceiq.flow.reactor.api.event.BaseNamedFlowEvent;
import com.sequenceiq.freeipa.api.v1.freeipa.stack.model.scale.VerticalScaleRequest;

@JsonDeserialize(builder = EnvironmentVerticalScaleEvent.Builder.class)
public class EnvironmentVerticalScaleEvent extends BaseNamedFlowEvent {

    private final VerticalScaleRequest freeIPAVerticalScaleRequest;

    public EnvironmentVerticalScaleEvent(String selector, Long resourceId, String resourceName, String resourceCrn,
            VerticalScaleRequest freeIPAVerticalScaleRequest) {
        super(selector, resourceId, resourceName, resourceCrn);
        this.freeIPAVerticalScaleRequest = freeIPAVerticalScaleRequest;
    }

    public EnvironmentVerticalScaleEvent(String selector, Long resourceId, Promise<AcceptResult> accepted, String resourceName,
            String resourceCrn, VerticalScaleRequest freeIPAVerticalScaleRequest) {
        super(selector, resourceId, accepted, resourceName, resourceCrn);
        this.freeIPAVerticalScaleRequest = freeIPAVerticalScaleRequest;
    }

    public VerticalScaleRequest getFreeIPAVerticalScaleRequest() {
        return freeIPAVerticalScaleRequest;
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

        private VerticalScaleRequest freeIPAVerticalScaleRequest;

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

        public Builder withFreeIPAVerticalScaleRequest(VerticalScaleRequest freeIPAVerticalScaleRequest) {
            this.freeIPAVerticalScaleRequest = freeIPAVerticalScaleRequest;
            return this;
        }

        public EnvironmentVerticalScaleEvent build() {
            return new EnvironmentVerticalScaleEvent(selector, resourceId, accepted, resourceName, resourceCrn, freeIPAVerticalScaleRequest);
        }
    }
}
