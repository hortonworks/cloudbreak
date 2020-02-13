package com.sequenceiq.cloudbreak.tag;

public class TagPreparationObject {

    private final String cloudPlatform;

    private final String userName;

    private final String userCrn;

    private final String accountId;

    private final String resourceCrn;

    private TagPreparationObject(Builder builder) {
        cloudPlatform = builder.cloudPlatform;
        userName = builder.user;
        userCrn = builder.userCrn;
        accountId = builder.accountId;
        resourceCrn = builder.resourceCrn;
    }

    public String getCloudPlatform() {
        return cloudPlatform;
    }

    public String getUserName() {
        return userName;
    }

    public String getUserCrn() {
        return userCrn;
    }

    public String getAccountId() {
        return accountId;
    }

    public String getResourceCrn() {
        return resourceCrn;
    }

    public static class Builder {

        private String cloudPlatform;

        private String user;

        private String userCrn;

        private String accountId;

        private String resourceCrn;

        public static Builder builder() {
            return new Builder();
        }

        public Builder withCloudPlatform(String cloudPlatform) {
            this.cloudPlatform = cloudPlatform;
            return this;
        }

        public Builder withUserName(String user) {
            this.user = user;
            return this;
        }

        public Builder withUserCrn(String userCrn) {
            this.userCrn = userCrn;
            return this;
        }

        public Builder withAccountId(String accountId) {
            this.accountId = accountId;
            return this;
        }

        public Builder withResourceCrn(String resourceCrn) {
            this.resourceCrn = resourceCrn;
            return this;
        }

        public TagPreparationObject build() {
            return new TagPreparationObject(this);
        }
    }

}
