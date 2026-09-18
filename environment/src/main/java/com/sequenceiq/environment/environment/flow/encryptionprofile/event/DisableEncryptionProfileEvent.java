package com.sequenceiq.environment.environment.flow.encryptionprofile.event;

import java.util.List;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonPOJOBuilder;
import com.sequenceiq.cloudbreak.common.event.AcceptResult;
import com.sequenceiq.cloudbreak.common.event.Selectable;
import com.sequenceiq.cloudbreak.eventbus.Promise;
import com.sequenceiq.flow.reactor.api.event.BaseNamedFlowEvent;

@JsonDeserialize(builder = DisableEncryptionProfileEvent.Builder.class)
public class DisableEncryptionProfileEvent extends BaseNamedFlowEvent implements Selectable {

    private final List<String> skippedClusters;

    public DisableEncryptionProfileEvent(String selector, Long resourceId, String resourceName, String resourceCrn) {
        super(selector, resourceId, resourceName, resourceCrn);
        this.skippedClusters = List.of();
    }

    public DisableEncryptionProfileEvent(String selector, Long resourceId, Promise<AcceptResult> accepted, String resourceName, String resourceCrn,
            List<String> skippedClusters) {
        super(selector, resourceId, accepted, resourceName, resourceCrn);
        this.skippedClusters = skippedClusters == null ? List.of() : List.copyOf(skippedClusters);
    }

    public List<String> getSkippedClusters() {
        return skippedClusters;
    }

    public static DisableEncryptionProfileEvent.Builder builder() {
        return new DisableEncryptionProfileEvent.Builder();
    }

    @JsonPOJOBuilder
    public static final class Builder {
        private String resourceName;

        private String resourceCrn;

        private String selector;

        private Long resourceId;

        private Promise<AcceptResult> accepted;

        private List<String> skippedClusters;

        private Builder() {
        }

        public DisableEncryptionProfileEvent.Builder withSkippedClusters(List<String> skippedClusters) {
            this.skippedClusters = skippedClusters;
            return this;
        }

        public DisableEncryptionProfileEvent.Builder withResourceName(String resourceName) {
            this.resourceName = resourceName;
            return this;
        }

        public DisableEncryptionProfileEvent.Builder withSelector(String selector) {
            this.selector = selector;
            return this;
        }

        public DisableEncryptionProfileEvent.Builder withResourceId(Long resourceId) {
            this.resourceId = resourceId;
            return this;
        }

        public DisableEncryptionProfileEvent.Builder withAccepted(Promise<AcceptResult> accepted) {
            this.accepted = accepted;
            return this;
        }

        public DisableEncryptionProfileEvent.Builder withResourceCrn(String resourceCrn) {
            this.resourceCrn = resourceCrn;
            return this;
        }

        public DisableEncryptionProfileEvent build() {
            return new DisableEncryptionProfileEvent(selector, resourceId, accepted, resourceName, resourceCrn, skippedClusters);
        }
    }
}
