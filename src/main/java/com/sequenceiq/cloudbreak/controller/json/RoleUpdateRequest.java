package com.sequenceiq.cloudbreak.controller.json;

import com.sequenceiq.cloudbreak.domain.UserRole;

public class RoleUpdateRequest implements UpdateRequest {
    private UserRole userRole;

    public UserRole getUserRole() {
        return userRole;
    }

    public void setUserRole(UserRole userRole) {
        this.userRole = userRole;
    }

    @Override
    public String toString() {
        return "RoleUpdateRequest{" +
                "userRole=" + userRole +
                '}';
    }
}
