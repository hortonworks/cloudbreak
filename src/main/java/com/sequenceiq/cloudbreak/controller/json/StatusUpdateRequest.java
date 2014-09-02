package com.sequenceiq.cloudbreak.controller.json;

import com.sequenceiq.cloudbreak.domain.UserStatus;

public class StatusUpdateRequest implements UpdateRequest {
    private UserStatus userStatus;

    public UserStatus getUserStatus() {
        return userStatus;
    }

    public void setUserStatus(UserStatus userStatus) {
        this.userStatus = userStatus;
    }

    @Override
    public String toString() {
        return "StatusUpdateRequest{"
                + "userStatus="
                + userStatus
                + '}';
    }
}
