package com.sequenceiq.cloudbreak.sigmadbus.model;

import java.util.HashMap;
import java.util.Map;

import org.apache.commons.collections.MapUtils;

public class DatabusRequestContext {

    private final String accountId;

    private final String environmentCrn;

    private final String resourceCrn;

    private final String resourceName;

    private final Map<String, String> additionalDatabusHeaders;

    private DatabusRequestContext(Builder builder) {
        this.accountId = builder.accountId;
        this.environmentCrn = builder.environmentCrn;
        this.resourceCrn = builder.resourceCrn;
        this.resourceName = builder.resourceName;
        this.additionalDatabusHeaders = MapUtils.isEmpty(builder.additionalDatabusHeaders)
                ? new HashMap<>() : builder.additionalDatabusHeaders;
    }

    public String getAccountId() {
        return accountId;
    }

    public String getEnvironmentCrn() {
        return environmentCrn;
    }

    public String getResourceCrn() {
        return resourceCrn;
    }

    public String getResourceName() {
        return resourceName;
    }

    public Map<String, String> getAdditionalDatabusHeaders() {
        return additionalDatabusHeaders;
    }

    @Override
    public String toString() {
        return "DatabusRequestContext{" +
                "accountId='" + accountId + '\'' +
                ", environmentCrn='" + environmentCrn + '\'' +
                ", resourceCrn='" + resourceCrn + '\'' +
                ", resourceName='" + resourceName + '\'' +
                ", additionalDatabusHeaders=" + additionalDatabusHeaders +
                '}';
    }

    public static class Builder {

        private String accountId;

        private String environmentCrn;

        private String resourceCrn;

        private String resourceName;

        private Map<String, String> additionalDatabusHeaders = new HashMap<>();

        private Builder() {
        }

        public static Builder newBuilder() {
            return new Builder();
        }

        public DatabusRequestContext build() {
            return new DatabusRequestContext(this);
        }

        public Builder withAccountId(String accountId) {
            this.accountId = accountId;
            return this;
        }

        public Builder withEnvironmentCrn(String environmentCrn) {
            this.environmentCrn = environmentCrn;
            return this;
        }

        public Builder withRecourceCrn(String recourceCrn) {
            this.resourceCrn = recourceCrn;
            return this;
        }

        public Builder withResourceName(String resourceName) {
            this.resourceName = resourceName;
            return this;
        }

        public Builder withAdditionalDatabusHeaders(Map<String, String> additionalDatabusHeaders) {
            this.additionalDatabusHeaders = additionalDatabusHeaders;
            return this;
        }

        public Builder addAdditionalDatabusHeader(String key, String value) {
            this.additionalDatabusHeaders.put(key, value);
            return this;
        }

    }
}
