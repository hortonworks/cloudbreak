package com.sequenceiq.cloudbreak.controller.json;

import com.sequenceiq.cloudbreak.domain.UserStatus;

public class UserActivationRequest implements JsonEntity {

    private UserStatus userStatus;

    public UserStatus getUserStatus() {
        return userStatus;
    }

    public void setUserStatus(UserStatus userStatus) {
        this.userStatus = userStatus;
    }
}
