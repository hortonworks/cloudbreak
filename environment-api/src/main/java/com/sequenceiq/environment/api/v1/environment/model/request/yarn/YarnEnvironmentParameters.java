package com.sequenceiq.environment.api.v1.environment.model.request.yarn;

import io.swagger.annotations.ApiModel;

@ApiModel(value = "YarnEnvironmentV1Parameters")
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
