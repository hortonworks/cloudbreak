package com.sequenceiq.environment.environment.flow.deletion.event;

import com.sequenceiq.flow.reactor.api.event.BaseNamedFlowEvent;

import reactor.rx.Promise;

public class EnvDeleteEvent extends BaseNamedFlowEvent {

    public EnvDeleteEvent(String selector, Long resourceId, String resourceName) {
        super(selector, resourceId, resourceName);
    }

    public EnvDeleteEvent(String selector, Long resourceId, Promise<Boolean> accepted, String resourceName) {
        super(selector, resourceId, accepted, resourceName);
    }

    public static final class EnvDeleteEventBuilder {
        private String resourceName;

        private String selector;

        private Long resourceId;

        private Promise<Boolean> accepted;

        private EnvDeleteEventBuilder() {
        }

        public static EnvDeleteEventBuilder anEnvDeleteEvent() {
            return new EnvDeleteEventBuilder();
        }

        public EnvDeleteEventBuilder withResourceName(String resourceName) {
            this.resourceName = resourceName;
            return this;
        }

        public EnvDeleteEventBuilder withSelector(String selector) {
            this.selector = selector;
            return this;
        }

        public EnvDeleteEventBuilder withResourceId(Long resourceId) {
            this.resourceId = resourceId;
            return this;
        }

        public EnvDeleteEventBuilder withAccepted(Promise<Boolean> accepted) {
            this.accepted = accepted;
            return this;
        }

        public EnvDeleteEvent build() {
            return new EnvDeleteEvent(selector, resourceId, accepted, resourceName);
        }
    }
}
