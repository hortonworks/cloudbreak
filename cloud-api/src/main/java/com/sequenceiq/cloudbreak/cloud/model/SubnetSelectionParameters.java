package com.sequenceiq.cloudbreak.cloud.model;

import com.sequenceiq.common.api.type.Tunnel;

public class SubnetSelectionParameters {

    private final Tunnel tunnel;

    private final boolean ha;

    private final boolean preferPrivateIfExist;

    private SubnetSelectionParameters(Tunnel tunnel, boolean ha, boolean preferPrivateIfExist) {
        this.tunnel = tunnel;
        this.ha = ha;
        this.preferPrivateIfExist = preferPrivateIfExist;
    }

    public Tunnel getTunnel() {
        return tunnel;
    }

    public boolean isHa() {
        return ha;
    }

    public boolean isPreferPrivateIfExist() {
        return preferPrivateIfExist;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private Tunnel tunnel = Tunnel.DIRECT;

        private boolean ha;

        private boolean preferPrivateIfExist;

        public Builder withTunnel(Tunnel tunnel) {
            this.tunnel = tunnel;
            return this;
        }

        public Builder withHa(boolean ha) {
            this.ha = ha;
            return this;
        }

        public Builder withPreferPrivateIfExist() {
            preferPrivateIfExist = true;
            return this;
        }

        public SubnetSelectionParameters build() {
            return new SubnetSelectionParameters(tunnel, ha, preferPrivateIfExist);
        }
    }
}
