package com.sequenceiq.environment.environment.flow.encryptionprofile.event;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonPOJOBuilder;
import com.sequenceiq.cloudbreak.common.event.AcceptResult;
import com.sequenceiq.cloudbreak.common.event.Selectable;
import com.sequenceiq.cloudbreak.eventbus.Promise;
import com.sequenceiq.flow.reactor.api.event.BaseNamedFlowEvent;

@JsonDeserialize(builder = EnableEncryptionProfileEvent.Builder.class)
public class EnableEncryptionProfileEvent extends BaseNamedFlowEvent implements Selectable {

    private final String encryptionProfileCrn;

    public EnableEncryptionProfileEvent(String selector, Long resourceId, String resourceName, String resourceCrn, String encryptionProfileCrn) {
        super(selector, resourceId, resourceName, resourceCrn);
        this.encryptionProfileCrn = encryptionProfileCrn;
    }

    public EnableEncryptionProfileEvent(String selector, Long resourceId, Promise<AcceptResult> accepted, String resourceName, String resourceCrn,
            String encryptionProfileCrn) {
        super(selector, resourceId, accepted, resourceName, resourceCrn);
        this.encryptionProfileCrn = encryptionProfileCrn;
    }

    public String getEncryptionProfileCrn() {
        return encryptionProfileCrn;
    }

    public static EnableEncryptionProfileEvent.Builder builder() {
        return new EnableEncryptionProfileEvent.Builder();
    }

    @JsonPOJOBuilder
    public static final class Builder {
        private String resourceName;

        private String resourceCrn;

        private String selector;

        private Long resourceId;

        private Promise<AcceptResult> accepted;

        private String encryptionProfileCrn;

        private Builder() {
        }

        public EnableEncryptionProfileEvent.Builder withResourceName(String resourceName) {
            this.resourceName = resourceName;
            return this;
        }

        public EnableEncryptionProfileEvent.Builder withSelector(String selector) {
            this.selector = selector;
            return this;
        }

        public EnableEncryptionProfileEvent.Builder withResourceId(Long resourceId) {
            this.resourceId = resourceId;
            return this;
        }

        public EnableEncryptionProfileEvent.Builder withAccepted(Promise<AcceptResult> accepted) {
            this.accepted = accepted;
            return this;
        }

        public EnableEncryptionProfileEvent.Builder withResourceCrn(String resourceCrn) {
            this.resourceCrn = resourceCrn;
            return this;
        }

        public EnableEncryptionProfileEvent.Builder withEncryptionProfileCrn(String encryptionProfileCrn) {
            this.encryptionProfileCrn = encryptionProfileCrn;
            return this;
        }

        public EnableEncryptionProfileEvent build() {
            return new EnableEncryptionProfileEvent(selector, resourceId, accepted, resourceName, resourceCrn, encryptionProfileCrn);
        }
    }
}
