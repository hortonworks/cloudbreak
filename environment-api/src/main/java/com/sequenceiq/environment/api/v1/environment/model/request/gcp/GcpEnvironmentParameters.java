package com.sequenceiq.environment.api.v1.environment.model.request.gcp;

import java.io.Serializable;

import io.swagger.annotations.ApiModel;

@ApiModel(value = "GcpEnvironmentV1Parameters")
public class GcpEnvironmentParameters implements Serializable {

    private GcpEnvironmentParameters(Builder builder) {
    }

    public static Builder builder() {
        return new Builder();
    }

    @Override
    public String toString() {
        return "GcpEnvironmentParameters{}";
    }

    public static final class Builder {

        private Builder() {
        }

        public GcpEnvironmentParameters build() {
            return new GcpEnvironmentParameters(this);
        }
    }
}
