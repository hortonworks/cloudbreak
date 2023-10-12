package com.sequenceiq.cloudbreak.cloud.model;

import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

public class ExtendedCloudCredential extends CloudCredential {

    private final String description;

    private final String cloudPlatform;

    private final String accountId;

    private final List<String> entitlements;

    private final CloudCredential cloudCredential;

    public ExtendedCloudCredential(CloudCredential cloudCredential, String cloudPlatform, String description, String accountId, List<String> entitlements) {
        super(cloudCredential.getId(), cloudCredential.getName(), accountId, cloudCredential.getCredentialSettings());
        Map<String, Object> parameters = cloudCredential.getParameters();
        for (Entry<String, Object> parameter : parameters.entrySet()) {
            putParameter(parameter.getKey(), parameter.getValue());
        }
        this.cloudPlatform = cloudPlatform;
        this.description = description;
        this.accountId = accountId;
        this.entitlements = entitlements;
        this.cloudCredential = cloudCredential;
    }

    public String getDescription() {
        return description;
    }

    public String getCloudPlatform() {
        return cloudPlatform;
    }

    public String getAccountId() {
        return accountId;
    }

    public List<String> getEntitlements() {
        return entitlements;
    }

    public CloudCredential getCloudCredential() {
        return cloudCredential;
    }

    // Must not reveal any secrets, hence not including DynamicModel.toString()!
    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder("ExtendedCloudCredential{");
        sb.append("description='").append(description).append('\'');
        sb.append(", cloudPlatform='").append(cloudPlatform).append('\'');
        sb.append(", accountId='").append(accountId).append('\'');
        sb.append(", cloudCredential=").append(super.toString());
        sb.append('}');
        return sb.toString();
    }

}
