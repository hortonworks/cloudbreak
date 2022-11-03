package com.sequenceiq.environment.environment.flow.upgrade.ccm.event;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonPOJOBuilder;
import com.sequenceiq.cloudbreak.common.event.AcceptResult;
import com.sequenceiq.cloudbreak.eventbus.Promise;
import com.sequenceiq.flow.reactor.api.event.BaseNamedFlowEvent;

@JsonDeserialize(builder = UpgradeCcmEvent.Builder.class)
public class UpgradeCcmEvent extends BaseNamedFlowEvent {

    private final String errorReason;

    public UpgradeCcmEvent(String selector, Long resourceId, String resourceName, String resourceCrn) {
        super(selector, resourceId, resourceName, resourceCrn);
        this.errorReason = null;
    }

    public UpgradeCcmEvent(String selector, Long resourceId, Promise<AcceptResult> accepted, String resourceName, String resourceCrn, String errorReason) {
        super(selector, resourceId, accepted, resourceName, resourceCrn);
        this.errorReason = errorReason;
    }

    public String getErrorReason() {
        return errorReason;
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

        private String errorReason;

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

        public Builder withErrorReason(String errorReason) {
            this.errorReason = errorReason;
            return this;
        }

        public UpgradeCcmEvent build() {
            return new UpgradeCcmEvent(selector, resourceId, accepted, resourceName, resourceCrn, errorReason);
        }
    }
}
