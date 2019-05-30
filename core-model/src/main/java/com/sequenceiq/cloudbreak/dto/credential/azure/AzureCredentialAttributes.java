package com.sequenceiq.cloudbreak.dto.credential.azure;

import java.io.Serializable;

public class AzureCredentialAttributes implements Serializable {

    private final String subscriptionId;

    private final String tenantId;

    private final String accessKey;

    private final AzureRoleBasedAttributes roleBased;

    private AzureCredentialAttributes(Builder builder) {
        subscriptionId = builder.subscriptionId;
        tenantId = builder.tenantId;
        accessKey = builder.accessKey;
        roleBased = builder.roleBased;
    }

    public String getSubscriptionId() {
        return subscriptionId;
    }

    public String getTenantId() {
        return tenantId;
    }

    public String getAccessKey() {
        return accessKey;
    }

    public AzureRoleBasedAttributes getRoleBased() {
        return roleBased;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static final class Builder {
        private String subscriptionId;

        private String tenantId;

        private String accessKey;

        private AzureRoleBasedAttributes roleBased;

        public Builder subscriptionId(String subscriptionId) {
            this.subscriptionId = subscriptionId;
            return this;
        }

        public Builder tenantId(String tenantId) {
            this.tenantId = tenantId;
            return this;
        }

        public Builder accessKey(String accessKey) {
            this.accessKey = accessKey;
            return this;
        }

        public Builder roleBased(AzureRoleBasedAttributes roleBased) {
            this.roleBased = roleBased;
            return this;
        }

        public AzureCredentialAttributes build() {
            return new AzureCredentialAttributes(this);
        }
    }
}