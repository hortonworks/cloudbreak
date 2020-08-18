package com.sequenceiq.environment.api.v1.environment.model.request.gcp;

import io.swagger.annotations.ApiModel;

@ApiModel(value = "GcpEnvironmentV1Parameters")
public class GcpEnvironmentParameters {

    private GcpEnvironmentParameters(GcpEnvironmentParameters.Builder builder) {
    }

    public static Builder builder() {
        return new Builder();
    }

    public static final class Builder {

        public GcpEnvironmentParameters build() {
            return new GcpEnvironmentParameters(this);
        }
    }
}
