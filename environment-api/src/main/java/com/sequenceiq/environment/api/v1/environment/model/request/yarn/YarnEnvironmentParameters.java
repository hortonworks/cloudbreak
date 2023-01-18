package com.sequenceiq.environment.api.v1.environment.model.request.yarn;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(name = "YarnEnvironmentV1Parameters")
@JsonIgnoreProperties(ignoreUnknown = true)
public class YarnEnvironmentParameters {

    public YarnEnvironmentParameters() {
    }

    private YarnEnvironmentParameters(Builder builder) {
    }

    public static Builder builder() {
        return new Builder();
    }

    @Override
    public String toString() {
        return "YarnEnvironmentParameters{}";
    }

    public static final class Builder {

        private Builder() {
        }

        public YarnEnvironmentParameters build() {
            return new YarnEnvironmentParameters(this);
        }
    }
}
