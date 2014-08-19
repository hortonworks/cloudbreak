package com.sequenceiq.cloudbreak.controller.json;

import com.sequenceiq.cloudbreak.domain.UserRole;

public class RoleUpdateRequest extends UserUpdateRequest {
    private UserRole userRole;

    public UserRole getUserRole() {
        return userRole;
    }

    public void setUserRole(UserRole userRole) {
        this.userRole = userRole;
    }
}
