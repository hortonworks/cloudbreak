package com.sequenceiq.cloudbreak.cloud.model;

import com.sequenceiq.common.api.type.Tunnel;

public class SubnetSelectionParameters {

    private final Tunnel tunnel;

    private final boolean ha;

    private final boolean preferPrivateNetwork;

    private SubnetSelectionParameters(Tunnel tunnel, boolean ha, boolean preferPrivateNetwork) {
        this.tunnel = tunnel;
        this.ha = ha;
        this.preferPrivateNetwork = preferPrivateNetwork;
    }

    public Tunnel getTunnel() {
        return tunnel;
    }

    public boolean isHa() {
        return ha;
    }

    public boolean isPreferPrivateNetwork() {
        return preferPrivateNetwork;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private Tunnel tunnel = Tunnel.DIRECT;

        private boolean ha;

        private boolean preferPrivateNetwork;

        public Builder withTunnel(Tunnel tunnel) {
            this.tunnel = tunnel;
            return this;
        }

        public Builder withHa(boolean ha) {
            this.ha = ha;
            return this;
        }

        public Builder withPreferPrivateNetwork() {
            preferPrivateNetwork = true;
            return this;
        }

        public SubnetSelectionParameters build() {
            return new SubnetSelectionParameters(tunnel, ha, preferPrivateNetwork);
        }
    }
}
