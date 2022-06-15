package com.sequenceiq.node.health.client.model;

public class CdpNodeStatusRequest {

    private final boolean skipObjectMapping;

    private final boolean metering;

    private final boolean cmMonitoring;

    private final boolean networkOnly;

    public CdpNodeStatusRequest(Builder builder) {
        this.metering = builder.metering;
        this.cmMonitoring = builder.cmMonitoring;
        this.skipObjectMapping = builder.skipObjectMapping;
        this.networkOnly = builder.networkOnly;
    }

    public boolean isMetering() {
        return metering;
    }

    public boolean isCmMonitoring() {
        return cmMonitoring;
    }

    public boolean isSkipObjectMapping() {
        return skipObjectMapping;
    }

    public boolean isNetworkOnly() {
        return networkOnly;
    }

    public static class Builder {

        private boolean metering;

        private boolean cmMonitoring;

        private boolean skipObjectMapping;

        private boolean networkOnly;

        private Builder() {
        }

        public static CdpNodeStatusRequest.Builder builder() {
            return new CdpNodeStatusRequest.Builder();
        }

        public CdpNodeStatusRequest build() {
            return new CdpNodeStatusRequest(this);
        }

        public Builder withMetering() {
            this.metering = true;
            return this;
        }

        public Builder withCmMonitoring() {
            this.cmMonitoring = true;
            return this;
        }

        public Builder withSkipObjectMapping() {
            this.skipObjectMapping = true;
            return this;
        }

        public Builder withNetworkOnly() {
            this.networkOnly = true;
            return this;
        }

    }
}
