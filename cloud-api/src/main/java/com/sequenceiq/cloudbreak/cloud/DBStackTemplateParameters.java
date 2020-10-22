package com.sequenceiq.cloudbreak.cloud;

import com.sequenceiq.common.model.PrivateEndpointType;

public class DBStackTemplateParameters {
    private final PrivateEndpointType privateEndpointType;

    private DBStackTemplateParameters(Builder builder) {
        this.privateEndpointType = builder.privateEndpointType;
    }

    public PrivateEndpointType getEndpointType() {
        return privateEndpointType;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private PrivateEndpointType privateEndpointType;

        public Builder setEndpointType(PrivateEndpointType privateEndpointType) {
            this.privateEndpointType = privateEndpointType;
            return this;
        }

        public DBStackTemplateParameters build() {
            return new DBStackTemplateParameters(this);
        }
    }
}
