package com.sequenceiq.cloudbreak.core.flow2.event;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonPOJOBuilder;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.StackVerticalScaleV4Request;
import com.sequenceiq.cloudbreak.reactor.api.event.StackEvent;

@JsonDeserialize(builder = CoreVerticalScalingTriggerEvent.Builder.class)
public class CoreVerticalScalingTriggerEvent extends StackEvent {

    private final StackVerticalScaleV4Request request;

    public CoreVerticalScalingTriggerEvent(
            @JsonProperty("selector") String selector,
            @JsonProperty("stackId") Long stackId,
            @JsonProperty("request") StackVerticalScaleV4Request request) {
        super(selector, stackId);
        this.request = request;
    }

    public StackVerticalScaleV4Request getRequest() {
        return request;
    }

    @JsonPOJOBuilder
    public static final class Builder {

        private StackVerticalScaleV4Request request;

        private  Long stackId;

        private String selector;

        private Builder() {
        }

        public static Builder anBuilder() {
            return new Builder();
        }

        public Builder withRequest(StackVerticalScaleV4Request request) {
            this.request = request;
            return this;
        }

        public Builder withStackId(Long stackId) {
            this.stackId = stackId;
            return this;
        }

        public Builder withSelector(String selector) {
            this.selector = selector;
            return this;
        }

        public CoreVerticalScalingTriggerEvent build() {
            return new CoreVerticalScalingTriggerEvent(selector, stackId, request);
        }
    }
}
