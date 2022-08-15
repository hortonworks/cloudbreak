package com.sequenceiq.datalake.flow.verticalscale.event;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonPOJOBuilder;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.StackVerticalScaleV4Request;
import com.sequenceiq.cloudbreak.common.event.AcceptResult;
import com.sequenceiq.cloudbreak.common.event.Selectable;
import com.sequenceiq.flow.reactor.api.event.BaseNamedFlowEvent;

import reactor.rx.Promise;

@JsonDeserialize(builder = DataLakeVerticalScaleEvent.Builder.class)
public class DataLakeVerticalScaleEvent extends BaseNamedFlowEvent implements Selectable {

    private final StackVerticalScaleV4Request verticalScaleRequest;

    private final String stackCrn;

    public DataLakeVerticalScaleEvent(
            @JsonProperty("selector") String selector,
            @JsonProperty("resourceId") Long resourceId,
            @JsonProperty("accepted") Promise<AcceptResult> accepted,
            @JsonProperty("resourceName") String resourceName,
            @JsonProperty("resourceCrn") String resourceCrn,
            @JsonProperty("stackCrn") String stackCrn,
            @JsonProperty("verticalScaleRequest") StackVerticalScaleV4Request verticalScaleRequest) {
        super(selector, resourceId, accepted, resourceName, resourceCrn);
        this.verticalScaleRequest = verticalScaleRequest;
        this.stackCrn = stackCrn;
    }

    public StackVerticalScaleV4Request getVerticalScaleRequest() {
        return verticalScaleRequest;
    }

    public String getStackCrn() {
        return stackCrn;
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

        private StackVerticalScaleV4Request verticalScaleRequest;

        private String stackCrn;

        private Builder() {
        }

        public Builder withResourceName(String resourceName) {
            this.resourceName = resourceName;
            return this;
        }

        public Builder withStackCrn(String stackCrn) {
            this.stackCrn = stackCrn;
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

        public Builder withVerticalScaleRequest(StackVerticalScaleV4Request verticalScaleRequest) {
            this.verticalScaleRequest = verticalScaleRequest;
            return this;
        }

        public DataLakeVerticalScaleEvent build() {
            return new DataLakeVerticalScaleEvent(selector, resourceId, accepted, resourceName, resourceCrn, stackCrn, verticalScaleRequest);
        }
    }
}
