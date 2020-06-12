package com.sequenceiq.cloudbreak.cloud.model;

import com.sequenceiq.common.api.type.Tunnel;

public class SubnetSelectionParameters {

    private final Tunnel tunnel;

    private final boolean ha;

    private final boolean preferPrivateIfExist;

    private final boolean internalTenant;

    private SubnetSelectionParameters(Tunnel tunnel, boolean ha, boolean preferPrivateIfExist, boolean internalTenant) {
        this.tunnel = tunnel;
        this.ha = ha;
        this.preferPrivateIfExist = preferPrivateIfExist;
        this.internalTenant = internalTenant;
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

    public boolean isInternalTenant() {
        return internalTenant;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private Tunnel tunnel = Tunnel.DIRECT;

        private boolean ha;

        private boolean preferPrivateIfExist;

        private boolean internalTenant;

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

        public Builder withIsInternalTenant(boolean internalTenant) {
            this.internalTenant = internalTenant;
            return this;
        }

        public SubnetSelectionParameters build() {
            return new SubnetSelectionParameters(tunnel, ha, preferPrivateIfExist, internalTenant);
        }
    }
}
