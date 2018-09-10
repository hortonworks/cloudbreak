package com.sequenceiq.cloudbreak.startup;

import java.util.Map;

import com.sequenceiq.cloudbreak.domain.workspace.Workspace;
import com.sequenceiq.cloudbreak.domain.workspace.User;

public class UserMigrationResults {

    private final Map<String, User> ownerIdToUser;

    private final Workspace orgForOrphanedResources;

    public UserMigrationResults(Map<String, User> ownerIdToUser, Workspace orgForOrphanedResources) {
        this.ownerIdToUser = ownerIdToUser;
        this.orgForOrphanedResources = orgForOrphanedResources;
    }

    public Map<String, User> getOwnerIdToUser() {
        return ownerIdToUser;
    }

    public Workspace getWorkspaceForOrphanedResources() {
        return orgForOrphanedResources;
    }
}
