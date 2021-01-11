package com.sequenceiq.cloudbreak.cloud.model;

import java.util.Set;

import com.sequenceiq.common.api.type.Tunnel;

public class SubnetSelectionParameters {

    private final Tunnel tunnel;

    private final boolean ha;

    private final boolean preferPrivateIfExist;

    private final Set<String> availabilityZones;

    private SubnetSelectionParameters(Tunnel tunnel, boolean ha, boolean preferPrivateIfExist, Set<String> availabilityZones) {
        this.tunnel = tunnel;
        this.ha = ha;
        this.preferPrivateIfExist = preferPrivateIfExist;
        this.availabilityZones = availabilityZones;
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

    public Set<String> getAvailabilityZones() {
        return availabilityZones;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private Tunnel tunnel = Tunnel.DIRECT;

        private boolean ha;

        private boolean preferPrivateIfExist;

        private Set<String> requiredAvailabilityZones = Set.of();

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

        public Builder withRequiredAvailabilityZones(Set<String> requiredAvailabilityZones) {
            this.requiredAvailabilityZones = requiredAvailabilityZones;
            return this;
        }

        public SubnetSelectionParameters build() {
            return new SubnetSelectionParameters(tunnel, ha, preferPrivateIfExist, requiredAvailabilityZones);
        }
    }
}
