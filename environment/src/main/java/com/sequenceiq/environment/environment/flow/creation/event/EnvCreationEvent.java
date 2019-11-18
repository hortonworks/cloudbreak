package com.sequenceiq.environment.environment.flow.creation.event;

import com.sequenceiq.flow.reactor.api.event.BaseNamedFlowEvent;

import reactor.rx.Promise;

public class EnvCreationEvent extends BaseNamedFlowEvent {

    public EnvCreationEvent(String selector, Long resourceId, String resourceName, String resourceCrn) {
        super(selector, resourceId, resourceName, resourceCrn);
    }

    public EnvCreationEvent(String selector, Long resourceId, Promise<Boolean> accepted, String resourceName, String resourceCrn) {
        super(selector, resourceId, accepted, resourceName, resourceCrn);
    }

    public static EnvCreationEventBuilder builder() {
        return new EnvCreationEventBuilder();
    }

    public static final class EnvCreationEventBuilder {
        private String resourceName;

        private String resourceCrn;

        private String selector;

        private Long resourceId;

        private Promise<Boolean> accepted;

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

        public EnvCreationEventBuilder withAccepted(Promise<Boolean> accepted) {
            this.accepted = accepted;
            return this;
        }

        public EnvCreationEvent build() {
            return new EnvCreationEvent(selector, resourceId, accepted, resourceName, resourceCrn);
        }
    }
}
