package com.sequenceiq.cloudbreak.cloud.model;

import java.util.Map;

public class ExtendedCloudCredential extends CloudCredential {

    private final String description;

    private final String owner;

    private final String account;

    private final String cloudPlatform;

    private final boolean publicInAccount;

    public ExtendedCloudCredential(Long id, String cloudPlatform, String name, String publicKey, String loginUserName, String description, String owner,
            String account, boolean publicInAccount) {
        super(id, name, publicKey, loginUserName);
        this.cloudPlatform = cloudPlatform;
        this.description = description;
        this.owner = owner;
        this.account = account;
        this.publicInAccount = publicInAccount;
    }

    public ExtendedCloudCredential(CloudCredential cloudCredential, String cloudPlatform, String description, String owner, String account,
            boolean publicInAccount) {
        super(cloudCredential.getId(), cloudCredential.getName(), cloudCredential.getPublicKey(), cloudCredential.getLoginUserName());
        Map<String, Object> parameters = cloudCredential.getParameters();
        for (Map.Entry<String, Object> parameter : parameters.entrySet()) {
            putParameter(parameter.getKey(), parameter.getValue());
        }
        this.cloudPlatform = cloudPlatform;
        this.description = description;
        this.owner = owner;
        this.account = account;
        this.publicInAccount = publicInAccount;
    }

    public String getDescription() {
        return description;
    }

    public String getOwner() {
        return owner;
    }

    public String getAccount() {
        return account;
    }

    public boolean isPublicInAccount() {
        return publicInAccount;
    }

    public String getCloudPlatform() {
        return cloudPlatform;
    }
}
