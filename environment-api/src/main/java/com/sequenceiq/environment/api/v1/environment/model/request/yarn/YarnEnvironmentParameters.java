package com.sequenceiq.environment.api.v1.environment.model.request.yarn;

import io.swagger.annotations.ApiModel;

@ApiModel(value = "YarnEnvironmentV1Parameters")
public class YarnEnvironmentParameters {

    private YarnEnvironmentParameters(YarnEnvironmentParameters.Builder builder) {
    }

    public static Builder builder() {
        return new Builder();
    }

    public static final class Builder {

        public YarnEnvironmentParameters build() {
            return new YarnEnvironmentParameters(this);
        }
    }
}
