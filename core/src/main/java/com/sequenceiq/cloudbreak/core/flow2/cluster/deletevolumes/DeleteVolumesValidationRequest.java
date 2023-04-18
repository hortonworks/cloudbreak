package com.sequenceiq.cloudbreak.core.flow2.cluster.deletevolumes;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonPOJOBuilder;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.StackDeleteVolumesRequest;
import com.sequenceiq.cloudbreak.reactor.api.event.StackEvent;

@JsonDeserialize(builder = DeleteVolumesValidationRequest.Builder.class)
public class DeleteVolumesValidationRequest extends StackEvent {

    private final StackDeleteVolumesRequest stackDeleteVolumesRequest;

    public DeleteVolumesValidationRequest(
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

        public static Builder builder() {
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

        public DeleteVolumesValidationRequest build() {
            return new DeleteVolumesValidationRequest(selector, stackId, stackDeleteVolumesRequest);
        }
    }
}
