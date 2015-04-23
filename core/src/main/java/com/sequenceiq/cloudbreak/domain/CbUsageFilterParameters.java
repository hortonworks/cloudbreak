package com.sequenceiq.cloudbreak.domain;

public class CbUsageFilterParameters {
    private String account;
    private String owner;
    private Long since;
    private String cloud;
    private String region;
    private Long filterEndDate;

    private CbUsageFilterParameters(Builder builder) {
        this.account = builder.account;
        this.owner = builder.owner;
        this.since = builder.since;
        this.cloud = builder.cloud;
        this.region = builder.region;
        this.filterEndDate = builder.filterEndDate;
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

    public Long getFilterEndDate() {
        return filterEndDate;
    }

    public static class Builder {
        private String account;
        private String owner;
        private Long since;
        private String cloud;
        private String region;
        private Long filterEndDate;

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

        public Builder setFilterEndDate(Long filterEndDate) {
            this.filterEndDate = filterEndDate;
            return this;
        }

        public CbUsageFilterParameters build() {
            return new CbUsageFilterParameters(this);
        }
    }
}
