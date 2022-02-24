package com.sequenceiq.cloudbreak.cloud.model;

import java.util.Map;
import java.util.Map.Entry;

public class ExtendedCloudCredential extends CloudCredential {

    private final String description;

    private final String cloudPlatform;

    private final String userCrn;

    private final String accountId;

    public ExtendedCloudCredential(CloudCredential cloudCredential, String cloudPlatform, String description, String userCrn, String accountId) {
        super(cloudCredential.getId(), cloudCredential.getName(), accountId, cloudCredential.isVerifyPermissions());
        Map<String, Object> parameters = cloudCredential.getParameters();
        for (Entry<String, Object> parameter : parameters.entrySet()) {
            putParameter(parameter.getKey(), parameter.getValue());
        }
        this.cloudPlatform = cloudPlatform;
        this.description = description;
        this.userCrn = userCrn;
        this.accountId = accountId;
    }

    public String getDescription() {
        return description;
    }

    public String getCloudPlatform() {
        return cloudPlatform;
    }

    public String getUserCrn() {
        return userCrn;
    }

    public String getAccountId() {
        return accountId;
    }

    // Must not reveal any secrets, hence not including DynamicModel.toString()!
    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder("ExtendedCloudCredential{");
        sb.append("description='").append(description).append('\'');
        sb.append(", cloudPlatform='").append(cloudPlatform).append('\'');
        sb.append(", userCrn='").append(userCrn).append('\'');
        sb.append(", accountId='").append(accountId).append('\'');
        sb.append(", cloudCredential=").append(super.toString());
        sb.append('}');
        return sb.toString();
    }

}
