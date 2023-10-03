package com.sequenceiq.environment.environment.dto.dataservices;

import java.io.Serializable;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonPOJOBuilder;

@JsonDeserialize(builder = AwsDataServiceParameters.Builder.class)
public record AwsDataServiceParameters() implements Serializable {

    public static Builder builder() {
        return new Builder();
    }

    @JsonPOJOBuilder
    public static class Builder {

        private Builder() {
        }

        public AwsDataServiceParameters build() {
            return new AwsDataServiceParameters();
        }
    }
}
