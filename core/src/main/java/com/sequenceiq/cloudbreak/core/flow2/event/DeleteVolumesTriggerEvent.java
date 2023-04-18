package com.sequenceiq.cloudbreak.core.flow2.event;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonPOJOBuilder;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.StackDeleteVolumesRequest;
import com.sequenceiq.cloudbreak.reactor.api.event.StackEvent;

@JsonDeserialize(builder = DeleteVolumesTriggerEvent.Builder.class)
public class DeleteVolumesTriggerEvent extends StackEvent {

    private final StackDeleteVolumesRequest stackDeleteVolumesRequest;

    public DeleteVolumesTriggerEvent(
            @JsonProperty("selector") String selector,
            @JsonProperty("stackId") Long stackId,
            @JsonProperty("stackDeleteVolumesRequest") StackDeleteVolumesRequest stackDeleteVolumesRequest) {
        super(selector, stackId);
        this.stackDeleteVolumesRequest = stackDeleteVolumesRequest;
    }

    public StackDeleteVolumesRequest getStackDeleteVolumesRequest() {
        return stackDeleteVolumesRequest;
    }

    @JsonPOJOBuilder
    public static final class Builder {

        private StackDeleteVolumesRequest stackDeleteVolumesRequest;

        private Long stackId;

        private String selector;

        private Builder() {
        }

        public static Builder anBuilder() {
            return new Builder();
        }

        public Builder withStackDeleteVolumesRequest(StackDeleteVolumesRequest stackDeleteVolumesRequest) {
            this.stackDeleteVolumesRequest = stackDeleteVolumesRequest;
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

        public DeleteVolumesTriggerEvent build() {
            return new DeleteVolumesTriggerEvent(selector, stackId, stackDeleteVolumesRequest);
        }
    }
}
