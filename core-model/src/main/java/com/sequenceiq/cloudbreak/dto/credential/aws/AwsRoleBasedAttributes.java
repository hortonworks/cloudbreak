package com.sequenceiq.cloudbreak.dto.credential.aws;

import java.io.Serializable;

public class AwsRoleBasedAttributes implements Serializable {

    private final String roleArn;

    private AwsRoleBasedAttributes(Builder builder) {
        roleArn = builder.roleArn;
    }

    public static Builder builder() {
        return new Builder();
    }

    public String getRoleArn() {
        return roleArn;
    }

    public static final class Builder {
        private String roleArn;

        public Builder roleArn(String roleArn) {
            this.roleArn = roleArn;
            return this;
        }

        public AwsRoleBasedAttributes build() {
            return new AwsRoleBasedAttributes(this);
        }
    }
}
