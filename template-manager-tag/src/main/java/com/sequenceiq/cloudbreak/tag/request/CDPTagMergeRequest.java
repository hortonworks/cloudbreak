package com.sequenceiq.cloudbreak.tag.request;

import java.util.Objects;

import com.google.common.base.Strings;
import com.sequenceiq.common.api.tag.model.Tags;

public class CDPTagMergeRequest {

    private final String platform;

    private final Tags environmentTags;

    private final Tags requestTags;

    private CDPTagMergeRequest(CDPTagMergeRequest.Builder builder) {
        this.platform = builder.platform;
        this.environmentTags = builder.environmentTags;
        this.requestTags = builder.requestTags;
    }

    public String getPlatform() {
        return platform;
    }

    public Tags getEnvironmentTags() {
        return environmentTags;
    }

    public Tags getRequestTags() {
        return requestTags;
    }

    public boolean isKeyNotPresented(String key) {
        return getRequestTags() == null || Strings.isNullOrEmpty(getRequestTags().getTagValue(key));
    }

    public static class Builder {

        private String platform;

        private Tags environmentTags = new Tags();

        private Tags requestTags = new Tags();

        public static Builder builder() {
            return new Builder();
        }

        public Builder withPlatform(String platform) {
            this.platform = platform;
            return this;
        }

        public Builder withEnvironmentTags(Tags environmentTags) {
            this.environmentTags = environmentTags;
            return this;
        }

        public Builder withRequestTags(Tags requestTags) {
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
