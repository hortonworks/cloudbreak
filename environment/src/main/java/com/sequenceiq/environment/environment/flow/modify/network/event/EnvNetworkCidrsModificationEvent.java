package com.sequenceiq.environment.environment.flow.modify.network.event;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonPOJOBuilder;
import com.sequenceiq.cloudbreak.common.event.AcceptResult;
import com.sequenceiq.cloudbreak.common.json.JsonIgnoreDeserialization;
import com.sequenceiq.cloudbreak.eventbus.Promise;
import com.sequenceiq.flow.reactor.api.event.BaseNamedFlowEvent;

public class EnvNetworkCidrsModificationEvent extends BaseNamedFlowEvent {

    private final List<String> networkCidrs;

    @JsonCreator
    public EnvNetworkCidrsModificationEvent(
            @JsonProperty("selector") String selector,
            @JsonProperty("resourceId") Long resourceId,
            @JsonProperty("resourceName") String resourceName,
            @JsonProperty("resourceCrn") String resourceCrn,
            @JsonProperty("networkCidrs") List<String> networkCidrs,
            @JsonIgnoreDeserialization @JsonProperty("accepted") Promise<AcceptResult> accepted) {
        super(selector, resourceId, accepted, resourceName, resourceCrn);
        this.networkCidrs = networkCidrs;
    }

    public List<String> getNetworkCidrs() {
        return networkCidrs;
    }

    public static EnvNetworkCidrsModificationEvent.Builder builder() {
        return new EnvNetworkCidrsModificationEvent.Builder();
    }

    @JsonPOJOBuilder
    public static final class Builder {
        private String selector;

        private Long resourceId;

        private String resourceName;

        private String resourceCrn;

        private List<String> networkCidrs;

        private Promise<AcceptResult> accepted;

        private Builder() {
        }

        public EnvNetworkCidrsModificationEvent.Builder withSelector(String selector) {
            this.selector = selector;
            return this;
        }

        public EnvNetworkCidrsModificationEvent.Builder withResourceId(Long resourceId) {
            this.resourceId = resourceId;
            return this;
        }

        public EnvNetworkCidrsModificationEvent.Builder withResourceName(String resourceName) {
            this.resourceName = resourceName;
            return this;
        }

        public EnvNetworkCidrsModificationEvent.Builder withResourceCrn(String resourceCrn) {
            this.resourceCrn = resourceCrn;
            return this;
        }

        public EnvNetworkCidrsModificationEvent.Builder withNetworkCidrs(List<String> networkCidrs) {
            this.networkCidrs = networkCidrs;
            return this;
        }

        public EnvNetworkCidrsModificationEvent.Builder withAccepted(Promise<AcceptResult> accepted) {
            this.accepted = accepted;
            return this;
        }

        public EnvNetworkCidrsModificationEvent build() {
            return new EnvNetworkCidrsModificationEvent(
                    selector,
                    resourceId,
                    resourceName,
                    resourceCrn,
                    networkCidrs,
                    accepted);
        }
    }
}
