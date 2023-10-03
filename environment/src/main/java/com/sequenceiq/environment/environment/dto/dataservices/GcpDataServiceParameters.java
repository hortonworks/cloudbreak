package com.sequenceiq.environment.environment.dto.dataservices;

import java.io.Serializable;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonPOJOBuilder;

@JsonDeserialize(builder = GcpDataServiceParameters.Builder.class)
public record GcpDataServiceParameters() implements Serializable {

    public static Builder builder() {
        return new Builder();
    }

    @JsonPOJOBuilder
    public static class Builder {

        private Builder() {
        }

        public GcpDataServiceParameters build() {
            return new GcpDataServiceParameters();
        }
    }
}
