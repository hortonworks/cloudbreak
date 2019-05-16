package com.sequenceiq.environment.env.flow.creation.event;

import com.sequenceiq.flow.reactor.api.event.BaseNamedFlowEvent;

import reactor.rx.Promise;

public class EnvCreationEvent extends BaseNamedFlowEvent {

    public EnvCreationEvent(String selector, Long resourceId, String resourceName) {
        super(selector, resourceId, resourceName);
    }

    public EnvCreationEvent(String selector, Long resourceId, Promise<Boolean> accepted, String resourceName) {
        super(selector, resourceId, accepted, resourceName);
    }

    public static final class EnvCreationEventBuilder {
        private String resourceName;

        private String selector;

        private Long resourceId;

        private Promise<Boolean> accepted;

        private EnvCreationEventBuilder() {
        }

        public static EnvCreationEventBuilder anEnvCreationEvent() {
            return new EnvCreationEventBuilder();
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

        public EnvCreationEventBuilder withAccepted(Promise<Boolean> accepted) {
            this.accepted = accepted;
            return this;
        }

        public EnvCreationEvent build() {
            return new EnvCreationEvent(selector, resourceId, accepted, resourceName);
        }
    }
}
