package com.sequenceiq.cloudbreak.usage.model;

public class UsageContext {

    private final String accountId;

    private final String resourceCrn;

    private UsageContext(Builder builder) {
        accountId = builder.accountId;
        resourceCrn = builder.resourceCrn;
    }

    public String getAccountId() {
        return accountId;
    }

    public String getResourceCrn() {
        return resourceCrn;
    }

    @Override
    public String toString() {
        return "UsageContext{" +
                "accountId='" + accountId + '\'' +
                ", resourceCrn='" + resourceCrn + '\'' +
                '}';
    }

    public static class Builder {

        private String accountId;

        private String resourceCrn;

        private Builder() {
        }

        public static Builder newBuilder() {
            return new Builder();
        }

        public UsageContext build() {
            return new UsageContext(this);
        }

        public Builder accountId(String accountId) {
            this.accountId = accountId;
            return this;
        }

        public Builder resourceCrn(String resourceCrn) {
            this.resourceCrn = resourceCrn;
            return this;
        }

    }
}
