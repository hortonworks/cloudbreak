package com.sequenceiq.cloudbreak.dto.credential.aws;

import java.io.Serializable;

public class AwsCredentialAttributes implements Serializable {

    private final AwsKeyBasedAttributes keyBased;

    private final AwsRoleBasedAttributes roleBased;

    private final Boolean govCloud;

    private AwsCredentialAttributes(Builder builder) {
        keyBased = builder.keyBased;
        roleBased = builder.roleBased;
        govCloud = builder.govCloud;
    }

    public AwsKeyBasedAttributes getKeyBased() {
        return keyBased;
    }

    public AwsRoleBasedAttributes getRoleBased() {
        return roleBased;
    }

    public Boolean isGovCloud() {
        return govCloud;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static final class Builder {
        private AwsKeyBasedAttributes keyBased;

        private AwsRoleBasedAttributes roleBased;

        private Boolean govCloud;

        public Builder keyBased(AwsKeyBasedAttributes keyBased) {
            this.keyBased = keyBased;
            return this;
        }

        public Builder roleBased(AwsRoleBasedAttributes roleBased) {
            this.roleBased = roleBased;
            return this;
        }

        public Builder govCloud(Boolean govCloud) {
            this.govCloud = govCloud;
            return this;
        }

        public AwsCredentialAttributes build() {
            return new AwsCredentialAttributes(this);
        }
    }
}
