package com.sequenceiq.environment.environment.flow.deletion.event;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonPOJOBuilder;
import com.sequenceiq.cloudbreak.common.event.AcceptResult;
import com.sequenceiq.cloudbreak.eventbus.Promise;
import com.sequenceiq.flow.reactor.api.event.BaseFlowEvent;
import com.sequenceiq.flow.reactor.api.event.BaseNamedFlowEvent;

@JsonDeserialize(builder = EnvDeleteEvent.Builder.class)
public class EnvDeleteEvent extends BaseNamedFlowEvent {

    private final boolean forceDelete;

    public EnvDeleteEvent(String selector, Long resourceId, String resourceName, String resourceCrn, boolean forceDelete) {
        super(selector, resourceId, resourceName, resourceCrn);
        this.forceDelete = forceDelete;
    }

    public EnvDeleteEvent(String selector, Long resourceId, Promise<AcceptResult> accepted, String resourceName, String resourceCrn, boolean forceDelete) {
        super(selector, resourceId, accepted, resourceName, resourceCrn);
        this.forceDelete = forceDelete;
    }

    public boolean isForceDelete() {
        return forceDelete;
    }

    @Override
    public boolean equalsEvent(BaseFlowEvent other) {
        return isClassAndEqualsEvent(EnvDeleteEvent.class, other,
                event -> forceDelete == event.forceDelete);
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

        private boolean forceDelete;

        private Promise<AcceptResult> accepted;

        private Builder() {
        }

        public Builder withResourceName(String resourceName) {
            this.resourceName = resourceName;
            return this;
        }

        public Builder withForceDelete(boolean forceDelete) {
            this.forceDelete = forceDelete;
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

        public EnvDeleteEvent build() {
            return new EnvDeleteEvent(selector, resourceId, accepted, resourceName, resourceCrn, forceDelete);
        }
    }
}
