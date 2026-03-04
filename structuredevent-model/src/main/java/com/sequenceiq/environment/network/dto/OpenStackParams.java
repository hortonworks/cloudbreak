package com.sequenceiq.environment.network.dto;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonPOJOBuilder;

@JsonDeserialize(builder = OpenStackParams.Builder.class)
public class OpenStackParams {

    private final String networkId;

    private final String routerId;

    private final String publicNetId;

    private OpenStackParams(Builder builder) {
        networkId = builder.networkId;
        routerId = builder.routerId;
        publicNetId = builder.publicNetId;
    }

    public String getNetworkId() {
        return networkId;
    }

    public String getRouterId() {
        return routerId;
    }

    public String getPublicNetId() {
        return publicNetId;
    }

    public static Builder builder() {
        return new Builder();
    }

    @JsonPOJOBuilder
    public static final class Builder {

        private String networkId;

        private String routerId;

        private String publicNetId;

        public Builder withNetworkId(String networkId) {
            this.networkId = networkId;
            return this;
        }

        public Builder withRouterId(String routerId) {
            this.routerId = routerId;
            return this;
        }

        public Builder withPublicNetId(String publicNetId) {
            this.publicNetId = publicNetId;
            return this;
        }

        public OpenStackParams build() {
            return new OpenStackParams(this);
        }
    }
}
