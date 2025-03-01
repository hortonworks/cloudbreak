package com.sequenceiq.cloudbreak.tag.request;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

import com.google.common.base.Strings;
import com.sequenceiq.common.model.DefaultApplicationTag;

public class CDPTagGenerationRequest {

    private final String platform;

    private final String environmentCrn;

    private final String resourceCrn;

    private final String resourceId;

    private final String creatorCrn;

    private final String userName;

    private final String accountId;

    private final boolean internalTenant;

    private final Map<String, String> sourceMap;

    private final Map<String, String> accountTags;

    private final Map<String, String> userDefinedTags;

    private CDPTagGenerationRequest(CDPTagGenerationRequest.Builder builder) {
        this.platform = builder.platform;
        this.environmentCrn = builder.environmentCrn;
        this.resourceCrn = builder.resourceCrn;
        this.creatorCrn = builder.creatorCrn;
        this.userName = builder.userName;
        this.sourceMap = builder.sourceMap;
        this.accountId = builder.accountId;
        this.internalTenant = builder.internalTenant;
        this.accountTags = builder.accountTags;
        this.userDefinedTags = builder.userDefinedTags;
        this.resourceId = builder.resourceId;
    }

    public String getPlatform() {
        return platform;
    }

    public String getEnvironmentCrn() {
        return environmentCrn;
    }

    public String getResourceCrn() {
        return resourceCrn;
    }

    public String getResourceId() {
        return resourceId;
    }

    public String getCreatorCrn() {
        return creatorCrn;
    }

    public String getUserName() {
        return userName;
    }

    public String getAccountId() {
        return accountId;
    }

    public Map<String, String> getSourceMap() {
        return sourceMap;
    }

    public boolean isInternalTenant() {
        return internalTenant;
    }

    public boolean isKeyNotPresented(DefaultApplicationTag tag) {
        return getSourceMap() == null || Strings.isNullOrEmpty(getSourceMap().get(tag.key()));
    }

    public Map<String, String> getAccountTags() {
        return accountTags;
    }

    public Map<String, String> getUserDefinedTags() {
        return userDefinedTags;
    }

    public static class Builder {

        private String platform;

        private String environmentCrn;

        private String resourceCrn;

        private String resourceId;

        private String creatorCrn;

        private String userName;

        private String accountId;

        private boolean internalTenant;

        private Map<String, String> sourceMap = new HashMap<>();

        private Map<String, String> accountTags = new HashMap<>();

        private Map<String, String> userDefinedTags = new HashMap<>();

        public static Builder builder() {
            return new Builder();
        }

        public Builder withPlatform(String platform) {
            this.platform = platform;
            return this;
        }

        public Builder withResourceCrn(String resourceCrn) {
            this.resourceCrn = resourceCrn;
            return this;
        }

        public Builder withResourceId(String resourceId) {
            this.resourceId = resourceId;
            return this;
        }

        public Builder withCreatorCrn(String creatorCrn) {
            this.creatorCrn = creatorCrn;
            return this;
        }

        public Builder withEnvironmentCrn(String environmentCrn) {
            this.environmentCrn = environmentCrn;
            return this;
        }

        public Builder withUserName(String userName) {
            this.userName = userName;
            return this;
        }

        public Builder withAccountId(String accountId) {
            this.accountId = accountId;
            return this;
        }

        public Builder withIsInternalTenant(boolean internalTenant) {
            this.internalTenant = internalTenant;
            return this;
        }

        public Builder withSourceMap(Map<String, String> sourceMap) {
            this.sourceMap = sourceMap;
            return this;
        }

        public Builder withAccountTags(Map<String, String> accountTags) {
            this.accountTags = accountTags;
            return this;
        }

        public Builder withUserDefinedTags(Map<String, String> userDefinedTags) {
            this.userDefinedTags = userDefinedTags;
            return this;
        }

        public CDPTagGenerationRequest build() {
            Objects.requireNonNull(platform);
            Objects.requireNonNull(environmentCrn);
            Objects.requireNonNull(resourceCrn);
            Objects.requireNonNull(creatorCrn);
            Objects.requireNonNull(userName);
            Objects.requireNonNull(accountId);
            return new CDPTagGenerationRequest(this);
        }
    }
}
