package com.sequenceiq.environment.api.v1.environment.model.request.openstack;

import io.swagger.annotations.ApiModel;

@ApiModel(value = "OpenstackEnvironmentV1Parameters")
public class OpenstackEnvironmentParameters {

    private OpenstackEnvironmentParameters(OpenstackEnvironmentParameters.Builder builder) {
    }

    public static Builder builder() {
        return new Builder();
    }

    public static final class Builder {

        public OpenstackEnvironmentParameters build() {
            return new OpenstackEnvironmentParameters(this);
        }
    }
}
