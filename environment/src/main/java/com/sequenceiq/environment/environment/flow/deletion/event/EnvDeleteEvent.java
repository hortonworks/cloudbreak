package com.sequenceiq.environment.environment.flow.deletion.event;

import com.sequenceiq.cloudbreak.common.event.AcceptResult;
import com.sequenceiq.flow.reactor.api.event.BaseNamedFlowEvent;

import reactor.rx.Promise;

public class EnvDeleteEvent extends BaseNamedFlowEvent {

    public EnvDeleteEvent(String selector, Long resourceId, String resourceName, String resourceCrn) {
        super(selector, resourceId, resourceName, resourceCrn);
    }

    public EnvDeleteEvent(String selector, Long resourceId, Promise<AcceptResult> accepted, String resourceName, String resourceCrn) {
        super(selector, resourceId, accepted, resourceName, resourceCrn);
    }

    public static EnvDeleteEventBuilder builder() {
        return new EnvDeleteEventBuilder();
    }

    public static final class EnvDeleteEventBuilder {
        private String resourceName;

        private String resourceCrn;

        private String selector;

        private Long resourceId;

        private Promise<AcceptResult> accepted;

        private EnvDeleteEventBuilder() {
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

        public EnvDeleteEventBuilder withAccepted(Promise<AcceptResult> accepted) {
            this.accepted = accepted;
            return this;
        }

        public EnvDeleteEventBuilder withResourceCrn(String resourceCrn) {
            this.resourceCrn = resourceCrn;
            return this;
        }

        public EnvDeleteEvent build() {
            return new EnvDeleteEvent(selector, resourceId, accepted, resourceName, resourceCrn);
        }
    }
}
