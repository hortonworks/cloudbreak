package com.sequenceiq.environment.environment.flow.stop.event;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonPOJOBuilder;
import com.sequenceiq.cloudbreak.common.event.AcceptResult;
import com.sequenceiq.cloudbreak.eventbus.Promise;
import com.sequenceiq.flow.reactor.api.event.BaseFlowEvent;
import com.sequenceiq.flow.reactor.api.event.BaseNamedFlowEvent;

@JsonDeserialize(builder = EnvStopEvent.EnvStopEventBuilder.class)
public class EnvStopEvent extends BaseNamedFlowEvent {

    public EnvStopEvent(String selector, Long resourceId, String resourceName, String resourceCrn) {
        super(selector, resourceId, resourceName, resourceCrn);
    }

    public EnvStopEvent(String selector, Long resourceId, Promise<AcceptResult> accepted, String resourceName, String resourceCrn) {
        super(selector, resourceId, accepted, resourceName, resourceCrn);
    }

    @Override
    public boolean equalsEvent(BaseFlowEvent other) {
        return isClassAndEqualsEvent(EnvStopEvent.class, other);
    }

    @JsonPOJOBuilder
    public static final class EnvStopEventBuilder {
        private String resourceName;

        private String resourceCrn;

        private String selector;

        private Long resourceId;

        private Promise<AcceptResult> accepted;

        private EnvStopEventBuilder() {
        }

        public static EnvStopEventBuilder anEnvStopEvent() {
            return new EnvStopEventBuilder();
        }

        public EnvStopEventBuilder withResourceName(String resourceName) {
            this.resourceName = resourceName;
            return this;
        }

        public EnvStopEventBuilder withSelector(String selector) {
            this.selector = selector;
            return this;
        }

        public EnvStopEventBuilder withResourceId(Long resourceId) {
            this.resourceId = resourceId;
            return this;
        }

        public EnvStopEventBuilder withAccepted(Promise<AcceptResult> accepted) {
            this.accepted = accepted;
            return this;
        }

        public EnvStopEventBuilder withResourceCrn(String resourceCrn) {
            this.resourceCrn = resourceCrn;
            return this;
        }

        public EnvStopEvent build() {
            return new EnvStopEvent(selector, resourceId, accepted, resourceName, resourceCrn);
        }
    }
}
