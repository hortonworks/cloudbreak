package com.sequenceiq.environment.environment.flow.hybrid.setupfinish.event;

import static com.sequenceiq.environment.environment.flow.hybrid.setupfinish.event.EnvironmentCrossRealmTrustSetupFinishStateSelectors.FAILED_TRUST_SETUP_FINISH_EVENT;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonPOJOBuilder;
import com.sequenceiq.cloudbreak.common.event.Selectable;
import com.sequenceiq.environment.environment.EnvironmentStatus;
import com.sequenceiq.flow.reactor.api.event.BaseFailedFlowEvent;

@JsonDeserialize(builder = EnvironmentCrossRealmTrustSetupFinishFailedEvent.Builder.class)
public class EnvironmentCrossRealmTrustSetupFinishFailedEvent extends BaseFailedFlowEvent implements Selectable {

    private final EnvironmentCrossRealmTrustSetupFinishEvent data;

    private final EnvironmentStatus environmentStatus;

    @JsonCreator
    public EnvironmentCrossRealmTrustSetupFinishFailedEvent(
            @JsonProperty("environmentSetupFinishCrossRealmTrustEvent") EnvironmentCrossRealmTrustSetupFinishEvent data,
            @JsonProperty("exception") Exception exception,
            @JsonProperty("environmentStatus") EnvironmentStatus environmentStatus) {
        super(FAILED_TRUST_SETUP_FINISH_EVENT.name(),
                data.getResourceId(),
                data.getResourceName(),
                data.getResourceCrn(),
                exception);
        this.data = data;
        this.environmentStatus = environmentStatus;
    }

    public EnvironmentCrossRealmTrustSetupFinishEvent getData() {
        return data;
    }

    public EnvironmentStatus getEnvironmentStatus() {
        return environmentStatus;
    }

    @JsonPOJOBuilder
    public static final class Builder {

        private EnvironmentCrossRealmTrustSetupFinishEvent data;

        private EnvironmentStatus environmentStatus;

        private Exception exception;

        private Builder() {
        }

        public Builder withData(EnvironmentCrossRealmTrustSetupFinishEvent data) {
            this.data = data;
            return this;
        }

        public Builder withEnvironmentStatus(EnvironmentStatus environmentStatus) {
            this.environmentStatus = environmentStatus;
            return this;
        }

        public Builder withException(Exception exception) {
            this.exception = exception;
            return this;
        }

        public EnvironmentCrossRealmTrustSetupFinishFailedEvent build() {
            return new EnvironmentCrossRealmTrustSetupFinishFailedEvent(data, exception, environmentStatus);
        }
    }
}
