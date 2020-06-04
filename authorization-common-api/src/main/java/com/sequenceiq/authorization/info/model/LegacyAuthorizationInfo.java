package com.sequenceiq.authorization.info.model;

import java.util.Set;

import com.google.common.collect.Sets;

public class LegacyAuthorizationInfo {

    private Set<String> permissionNeeded = Sets.newHashSet();

    public Set<String> getPermissionNeeded() {
        return permissionNeeded;
    }

    public void setPermissionNeeded(Set<String> permissionNeeded) {
        this.permissionNeeded = permissionNeeded;
    }
}
