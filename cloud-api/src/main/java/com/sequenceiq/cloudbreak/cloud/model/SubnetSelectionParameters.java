package com.sequenceiq.cloudbreak.cloud.model;

import com.sequenceiq.common.api.type.Tunnel;

public class SubnetSelectionParameters {

    private final Tunnel tunnel;

    private final boolean ha;

    private final boolean forDatabase;

    private SubnetSelectionParameters(Tunnel tunnel, boolean ha, boolean forDatabase) {
        this.tunnel = tunnel;
        this.ha = ha;
        this.forDatabase = forDatabase;
    }

    public Tunnel getTunnel() {
        return tunnel;
    }

    public boolean isHa() {
        return ha;
    }

    public boolean isForDatabase() {
        return forDatabase;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private Tunnel tunnel = Tunnel.DIRECT;

        private boolean ha;

        private boolean forDatabase;

        public Builder withTunnel(Tunnel tunnel) {
            this.tunnel = tunnel;
            return this;
        }

        public Builder withHa(boolean ha) {
            this.ha = ha;
            return this;
        }

        public Builder withForDatabase() {
            forDatabase = true;
            return this;
        }

        public SubnetSelectionParameters build() {
            return new SubnetSelectionParameters(tunnel, ha, forDatabase);
        }
    }
}
