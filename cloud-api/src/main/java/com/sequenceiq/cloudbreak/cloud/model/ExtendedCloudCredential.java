package com.sequenceiq.cloudbreak.cloud.model;

import java.util.Map;
import java.util.Map.Entry;

public class ExtendedCloudCredential extends CloudCredential {

    private final String description;

    private final String cloudPlatform;

    private final String userCrn;

    private final String accountId;

    public ExtendedCloudCredential(CloudCredential cloudCredential, String cloudPlatform, String description, String userCrn, String accountId) {
        super(cloudCredential.getId(), cloudCredential.getName(), cloudCredential.isVerifyPermissions());
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
}
