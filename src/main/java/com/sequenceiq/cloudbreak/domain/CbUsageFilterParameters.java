package com.sequenceiq.cloudbreak.domain;

public class CbUsageFilterParameters {
    private String account;
    private String owner;
    private Long since;
    private String cloud;
    private String region;
    private String vmType;
    private Long instanceHours;
    private Long bpId;
    private String bpName;

    private CbUsageFilterParameters(Builder builder) {
        this.account = builder.account;
        this.owner = builder.owner;
        this.since = builder.since;
        this.cloud = builder.cloud;
        this.region = builder.region;
        this.vmType = builder.vmType;
        this.instanceHours = builder.instanceHours;
        this.bpId = builder.bpId;
        this.bpName = builder.bpName;
    }

    public String getAccount() {
        return account;
    }

    public String getOwner() {
        return owner;
    }

    public Long getSince() {
        return since;
    }

    public String getCloud() {
        return cloud;
    }

    public String getRegion() {
        return region;
    }

    public String getVmType() {
        return vmType;
    }

    public Long getInstanceHours() {
        return instanceHours;
    }

    public Long getBpId() {
        return bpId;
    }

    public String getBpName() {
        return bpName;
    }

    public static class Builder {
        private String account;
        private String owner;
        private Long since;
        private String cloud;
        private String region;
        private String vmType;
        private Long instanceHours;
        private Long bpId;
        private String bpName;

        public Builder setAccount(String account) {
            this.account = account;
            return this;
        }

        public Builder setOwner(String owner) {
            this.owner = owner;
            return this;
        }

        public Builder setSince(Long since) {
            this.since = since;
            return this;
        }

        public Builder setCloud(String cloud) {
            this.cloud = cloud;
            return this;
        }

        public Builder setRegion(String region) {
            this.region = region;
            return this;
        }

        public Builder setVmType(String vmType) {
            this.vmType = vmType;
            return this;
        }

        public Builder setInstanceHours(Long instanceHours) {
            this.instanceHours = instanceHours;
            return this;
        }

        public Builder setBpId(Long bpId) {
            this.bpId = bpId;
            return this;
        }

        public Builder setBpName(String bpName) {
            this.bpName = bpName;
            return this;
        }

        public CbUsageFilterParameters build() {
            return new CbUsageFilterParameters(this);
        }
    }
}
