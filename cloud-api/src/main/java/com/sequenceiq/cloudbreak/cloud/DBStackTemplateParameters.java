package com.sequenceiq.cloudbreak.cloud;

import com.sequenceiq.common.model.EndpointType;

public class DBStackTemplateParameters {
    private final EndpointType endpointType;

    private DBStackTemplateParameters(Builder builder) {
        this.endpointType = builder.endpointType;
    }

    public EndpointType getEndpointType() {
        return endpointType;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private EndpointType endpointType;

        public Builder setEndpointType(EndpointType endpointType) {
            this.endpointType = endpointType;
            return this;
        }

        public DBStackTemplateParameters build() {
            return new DBStackTemplateParameters(this);
        }
    }
}
