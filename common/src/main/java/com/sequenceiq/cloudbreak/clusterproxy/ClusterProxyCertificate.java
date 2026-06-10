package com.sequenceiq.cloudbreak.clusterproxy;

public class ClusterProxyCertificate {

    private String signKey;

    private String signPub;

    private String signCert;

    private String signTokenCert;

    private ClusterProxyCertificate(Builder builder) {
        this.signKey = builder.signKey;
        this.signPub = builder.signPub;
        this.signCert = builder.signCert;
        this.signTokenCert = builder.signTokenCert;
    }

    public String getSignKey() {
        return signKey;
    }

    public String getSignPub() {
        return signPub;
    }

    public String getSignCert() {
        return signCert;
    }

    public String getSignTokenCert() {
        return signTokenCert;
    }

    public static Builder newClusterProxyCertificate() {
        return new Builder();
    }

    public static final class Builder {
        private String signKey;

        private String signPub;

        private String signCert;

        private String signTokenCert;

        private Builder() {
        }

        public Builder withSignKey(String signKey) {
            this.signKey = signKey;
            return this;
        }

        public Builder withSignPub(String signPub) {
            this.signPub = signPub;
            return this;
        }

        public Builder withSignCert(String signCert) {
            this.signCert = signCert;
            return this;
        }

        public Builder withSignTokenCert(String signTokenCert) {
            this.signTokenCert = signTokenCert;
            return this;
        }

        public ClusterProxyCertificate build() {
            return new ClusterProxyCertificate(this);
        }
    }
}
