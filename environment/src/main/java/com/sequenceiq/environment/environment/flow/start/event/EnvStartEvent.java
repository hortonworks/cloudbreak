package com.sequenceiq.environment.environment.flow.start.event;

import java.util.Objects;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonPOJOBuilder;
import com.sequenceiq.cloudbreak.common.event.AcceptResult;
import com.sequenceiq.cloudbreak.eventbus.Promise;
import com.sequenceiq.common.api.type.DataHubStartAction;
import com.sequenceiq.flow.reactor.api.event.BaseFlowEvent;
import com.sequenceiq.flow.reactor.api.event.BaseNamedFlowEvent;

@JsonDeserialize(builder = EnvStartEvent.Builder.class)
public class EnvStartEvent extends BaseNamedFlowEvent {

    private final DataHubStartAction dataHubStartAction;

    public EnvStartEvent(String selector, Long resourceId, String resourceName, String resourceCrn, DataHubStartAction dataHubStartAction) {
        super(selector, resourceId, resourceName, resourceCrn);
        this.dataHubStartAction = dataHubStartAction;
    }

    public EnvStartEvent(String selector, Long resourceId, Promise<AcceptResult> accepted, String resourceName,
        String resourceCrn, DataHubStartAction dataHubStartAction) {
        super(selector, resourceId, accepted, resourceName, resourceCrn);
        this.dataHubStartAction = dataHubStartAction;
    }

    @Override
    public boolean equalsEvent(BaseFlowEvent other) {
        return isClassAndEqualsEvent(EnvStartEvent.class, other,
                event -> Objects.equals(dataHubStartAction, event.dataHubStartAction));
    }

    public DataHubStartAction getDataHubStartAction() {
        return dataHubStartAction;
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

        private DataHubStartAction dataHubStartAction;

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

        public Builder withDataHubStartAction(DataHubStartAction dataHubStartAction) {
            this.dataHubStartAction = dataHubStartAction;
            return this;
        }

        public EnvStartEvent build() {
            return new EnvStartEvent(selector, resourceId, accepted, resourceName, resourceCrn, dataHubStartAction);
        }
    }
}
