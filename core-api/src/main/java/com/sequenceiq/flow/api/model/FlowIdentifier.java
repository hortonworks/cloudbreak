package com.sequenceiq.flow.api.model;

import java.util.Objects;

import org.apache.commons.lang3.Validate;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.sequenceiq.cloudbreak.api.endpoint.v4.JsonEntity;

public class FlowIdentifier implements JsonEntity {

    private final FlowType type;

    private final String pollableId;

    @JsonCreator
    public FlowIdentifier(@JsonProperty("type") FlowType type, @JsonProperty("pollableId") String pollableId) {
        this.type = Objects.requireNonNull(type);
        if (!FlowType.NOT_TRIGGERED.equals(type)) {
            this.pollableId = Validate.notBlank(pollableId, "pollableId must not be empty");
        } else {
            if (pollableId != null) {
                throw new IllegalArgumentException("Should not set pollable id when flow type is " + FlowType.NOT_TRIGGERED);
            }
            this.pollableId = null;
        }
    }

    public static FlowIdentifier notTriggered() {
        return new FlowIdentifier(FlowType.NOT_TRIGGERED, null);
    }

    public FlowType getType() {
        return type;
    }

    public String getPollableId() {
        return pollableId;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        FlowIdentifier that = (FlowIdentifier) o;
        return type == that.type &&
                Objects.equals(pollableId, that.pollableId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(type, pollableId);
    }

    @Override
    public String toString() {
        return "FlowIdentifier{" +
                "type=" + type +
                ", pollableId='" + pollableId + '\'' +
                '}';
    }
}
