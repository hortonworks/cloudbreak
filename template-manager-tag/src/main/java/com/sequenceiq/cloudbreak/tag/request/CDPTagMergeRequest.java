package com.sequenceiq.cloudbreak.tag.request;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

import com.google.common.base.Strings;

public class CDPTagMergeRequest {

    private final String platform;

    private final Map<String, String> environmentTags;

    private final Map<String, String> requestTags;

    private CDPTagMergeRequest(CDPTagMergeRequest.Builder builder) {
        this.platform = builder.platform;
        this.environmentTags = builder.environmentTags;
        this.requestTags = builder.requestTags;
    }

    public String getPlatform() {
        return platform;
    }

    public Map<String, String> getEnvironmentTags() {
        return environmentTags;
    }

    public Map<String, String> getRequestTags() {
        return requestTags;
    }

    public boolean isKeyNotPresented(String key) {
        return getRequestTags() == null || Strings.isNullOrEmpty(getRequestTags().get(key));
    }

    public static class Builder {

        private String platform;

        private Map<String, String> environmentTags = new HashMap<>();

        private Map<String, String> requestTags = new HashMap<>();

        public static Builder builder() {
            return new Builder();
        }

        public Builder withPlatform(String platform) {
            this.platform = platform;
            return this;
        }

        public Builder withEnvironmentTags(Map<String, String> environmentTags) {
            this.environmentTags = environmentTags;
            return this;
        }

        public Builder withRequestTags(Map<String, String> requestTags) {
            this.requestTags = requestTags;
            return this;
        }

        public CDPTagMergeRequest build() {
            Objects.requireNonNull(platform);
            Objects.requireNonNull(environmentTags);
            Objects.requireNonNull(requestTags);
            return new CDPTagMergeRequest(this);
        }
    }
}
