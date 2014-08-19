package com.sequenceiq.cloudbreak.controller.json;

import java.util.HashSet;
import java.util.Set;

import com.sequenceiq.cloudbreak.domain.UserRole;
import com.sequenceiq.cloudbreak.domain.UserStatus;

public class UserUpdateRequest implements JsonEntity {

    private UserStatus userStatus;

    private Set<UserRole> userRoles = new HashSet<>();

    public boolean isStatusUpdate() {
        return userStatus != null;
    }

    public boolean isRoleUpdate() {
        return !userRoles.isEmpty();
    }

    public UserStatus getUserStatus() {
        return userStatus;
    }

    public void setUserStatus(UserStatus userStatus) {
        this.userStatus = userStatus;
    }

    public Set<UserRole> getUserRoles() {
        return userRoles;
    }

    public void setUserRoles(Set<UserRole> userRoles) {
        this.userRoles = userRoles;
    }
}
