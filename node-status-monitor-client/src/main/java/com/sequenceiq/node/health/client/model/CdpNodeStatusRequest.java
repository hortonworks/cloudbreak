package com.sequenceiq.node.health.client.model;

public class CdpNodeStatusRequest {

    private final boolean metering;

    private final boolean cmMonitoring;

    public CdpNodeStatusRequest(Builder builder) {
        this.metering = builder.metering;
        this.cmMonitoring = builder.cmMonitoring;
    }

    public boolean isMetering() {
        return metering;
    }

    public boolean isCmMonitoring() {
        return cmMonitoring;
    }

    public static class Builder {

        private boolean metering;

        private boolean cmMonitoring;

        private Builder() {
        }

        public static CdpNodeStatusRequest.Builder builder() {
            return new CdpNodeStatusRequest.Builder();
        }

        public CdpNodeStatusRequest build() {
            return new CdpNodeStatusRequest(this);
        }

        public Builder withMetering(boolean metering) {
            this.metering = metering;
            return this;
        }

        public Builder withCmMonitoring(boolean cmMonitoring) {
            this.cmMonitoring = cmMonitoring;
            return this;
        }

    }
}
