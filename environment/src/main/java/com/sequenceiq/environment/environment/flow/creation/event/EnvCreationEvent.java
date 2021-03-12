package com.sequenceiq.environment.environment.flow.creation.event;

import com.sequenceiq.cloudbreak.common.event.AcceptResult;
import com.sequenceiq.flow.reactor.api.event.BaseNamedFlowEvent;

import reactor.rx.Promise;

public class EnvCreationEvent extends BaseNamedFlowEvent {

    private EnvCreationEvent(EnvCreationEventBuilder builder) {
        super(builder.selector, builder.resourceId, builder.accepted, builder.resourceName, builder.resourceCrn);
    }

    public static EnvCreationEventBuilder builder() {
        return new EnvCreationEventBuilder();
    }

    public static final class EnvCreationEventBuilder {
        private String resourceName;

        private String resourceCrn;

        private String selector;

        private Long resourceId;

        private Promise<AcceptResult> accepted;

        private EnvCreationEventBuilder() {
        }

        public EnvCreationEventBuilder withResourceName(String resourceName) {
            this.resourceName = resourceName;
            return this;
        }

        public EnvCreationEventBuilder withSelector(String selector) {
            this.selector = selector;
            return this;
        }

        public EnvCreationEventBuilder withResourceId(Long resourceId) {
            this.resourceId = resourceId;
            return this;
        }

        public EnvCreationEventBuilder withResourceCrn(String resourceCrn) {
            this.resourceCrn = resourceCrn;
            return this;
        }

        public EnvCreationEventBuilder withAccepted(Promise<AcceptResult> accepted) {
            this.accepted = accepted;
            return this;
        }

        public EnvCreationEvent build() {
            return new EnvCreationEvent(this);
        }
    }
}
