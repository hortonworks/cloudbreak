package com.sequenceiq.it.cloudbreak.assertion.audit;

import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

public class OperationInfo {

    private final String eventName;

    private final Set<String> firstStates;

    private final Set<String> lastStates;

    public OperationInfo(Builder builder) {
        eventName = builder.eventName;
        firstStates = builder.firstStates;
        lastStates = builder.lastStates;
    }

    public String getEventName() {
        return eventName;
    }

    public Set<String> getFirstStates() {
        return firstStates;
    }

    public Set<String> getLastStates() {
        return lastStates;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {

        private String eventName;

        private final Set<String> firstStates = new HashSet<>();

        private final Set<String> lastStates = new HashSet<>();

        public Builder withEventName(String eventName) {
            this.eventName = eventName;
            return this;
        }

        public Builder withFirstStates(String... firstStates) {
            this.firstStates.addAll(Set.of(firstStates));
            return this;
        }

        public Builder withLastStates(String... lastStates) {
            this.lastStates.addAll(Set.of(lastStates));
            return this;
        }

        public OperationInfo build() {
            Objects.requireNonNull(eventName);
            return new OperationInfo(this);
        }
    }
}
