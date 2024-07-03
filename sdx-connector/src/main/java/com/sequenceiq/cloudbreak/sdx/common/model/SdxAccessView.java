package com.sequenceiq.cloudbreak.sdx.common.model;

public record SdxAccessView(String clusterManagerFqdn,
                            String clusterManagerIp,
                            String rangerFqdn) {

    public static Builder builder() {
        return new Builder();
    }

    public static final class Builder {
        private String clusterManagerFqdn;

        private String clusterManagerIp;

        private String rangerFqdn;

        private Builder() {
        }

        public Builder withClusterManagerFqdn(String clusterManagerFqdn) {
            this.clusterManagerFqdn = clusterManagerFqdn;
            return this;
        }

        public Builder withClusterManagerIp(String clusterManagerIp) {
            this.clusterManagerIp = clusterManagerIp;
            return this;
        }

        public Builder withRangerFqdn(String rangerFqdn) {
            this.rangerFqdn = rangerFqdn;
            return this;
        }

        public SdxAccessView build() {
            return new SdxAccessView(this.clusterManagerFqdn, this.clusterManagerIp, this.rangerFqdn);
        }
    }
}
