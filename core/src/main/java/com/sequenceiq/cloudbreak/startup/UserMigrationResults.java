package com.sequenceiq.cloudbreak.startup;

import java.util.Map;

import com.sequenceiq.cloudbreak.domain.organization.Organization;
import com.sequenceiq.cloudbreak.domain.organization.User;

public class UserMigrationResults {

    private final Map<String, User> ownerIdToUser;

    private final Organization orgForOrphanedResources;

    public UserMigrationResults(Map<String, User> ownerIdToUser, Organization orgForOrphanedResources) {
        this.ownerIdToUser = ownerIdToUser;
        this.orgForOrphanedResources = orgForOrphanedResources;
    }

    public Map<String, User> getOwnerIdToUser() {
        return ownerIdToUser;
    }

    public Organization getOrgForOrphanedResources() {
        return orgForOrphanedResources;
    }
}
