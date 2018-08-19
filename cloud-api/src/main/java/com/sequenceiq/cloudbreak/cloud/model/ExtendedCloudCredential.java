package com.sequenceiq.cloudbreak.cloud.model;

import java.util.Map;
import java.util.Map.Entry;

import com.sequenceiq.cloudbreak.common.model.user.IdentityUser;

public class ExtendedCloudCredential extends CloudCredential {

    private final String description;

    private final String owner;

    private final String account;

    private final String cloudPlatform;

    private final boolean publicInAccount;

    private final IdentityUser identityUser;

    private final String userId;

    private final Long organziationId;

    public ExtendedCloudCredential(Long id, String cloudPlatform, String name, String description, String owner,
            String account, boolean publicInAccount, IdentityUser identityUser, String userId, Long organziationId) {
        super(id, name);
        this.cloudPlatform = cloudPlatform;
        this.description = description;
        this.owner = owner;
        this.account = account;
        this.publicInAccount = publicInAccount;
        this.identityUser = identityUser;
        this.userId = userId;
        this.organziationId = organziationId;
    }

    public ExtendedCloudCredential(CloudCredential cloudCredential, String cloudPlatform, String description, String owner, String account,
            boolean publicInAccount, IdentityUser identityUser, String userId, Long organziationId) {
        super(cloudCredential.getId(), cloudCredential.getName());
        Map<String, Object> parameters = cloudCredential.getParameters();
        for (Entry<String, Object> parameter : parameters.entrySet()) {
            putParameter(parameter.getKey(), parameter.getValue());
        }
        this.cloudPlatform = cloudPlatform;
        this.description = description;
        this.owner = owner;
        this.account = account;
        this.publicInAccount = publicInAccount;
        this.identityUser = identityUser;
        this.userId = userId;
        this.organziationId = organziationId;
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

    public IdentityUser getIdentityUser() {
        return identityUser;
    }

    public String getUserId() {
        return userId;
    }

    public Long getOrganziationId() {
        return organziationId;
    }
}
