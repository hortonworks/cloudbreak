package com.sequenceiq.cloudbreak.controller.json;

import com.sequenceiq.cloudbreak.domain.UserRole;
import com.sequenceiq.cloudbreak.domain.UserStatus;

public class UserUpdateRequest implements JsonEntity {

    private UserStatus userStatus;

    private UserRole userRole;

    public boolean isStatusUpdate() {
        return userStatus != null;
    }

    public boolean isRoleUpdate() {
        return userRole != null;
    }

    public UserStatus getUserStatus() {
        return userStatus;
    }

    public void setUserStatus(UserStatus userStatus) {
        this.userStatus = userStatus;
    }

    public UserRole getUserRole() {
        return userRole;
    }

    public void setUserRole(UserRole userRole) {
        this.userRole = userRole;
    }
}
