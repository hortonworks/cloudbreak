package com.sequenceiq.it.cloudbreak.assertion.audit;

import java.util.Objects;

public class OperationInfo {

    private final String eventName;

    private final String firstState;

    private final String lastState;

    public OperationInfo(Builder builder) {
        eventName = builder.eventName;
        firstState = builder.firstState;
        lastState = builder.lastState;
    }

    public String getEventName() {
        return eventName;
    }

    public String getFirstState() {
        return firstState;
    }

    public String getLastState() {
        return lastState;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {

        private String eventName;

        private String firstState;

        private String lastState;

        public Builder withEventName(String eventName) {
            this.eventName = eventName;
            return this;
        }

        public Builder withFirstState(String firstState) {
            this.firstState = firstState;
            return this;
        }

        public Builder withLastState(String lastState) {
            this.lastState = lastState;
            return this;
        }

        public OperationInfo build() {
            Objects.requireNonNull(eventName);
            return new OperationInfo(this);
        }
    }
}
