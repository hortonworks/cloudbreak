package com.sequenceiq.cloudbreak.service.cluster.event;

import com.sequenceiq.cloudbreak.cloud.model.Platform;

public class ClusterUserNamePasswordUpdateRequest {

    private final Long stackId;
    private final String newUserName;
    private final String newPassword;
    private final Platform cloudPlatform;

    public ClusterUserNamePasswordUpdateRequest(Long stackId, String newUserName, String newPassword, Platform cloudPlatform) {
        this.stackId = stackId;
        this.newUserName = newUserName;
        this.newPassword = newPassword;
        this.cloudPlatform = cloudPlatform;
    }

    public Long getStackId() {
        return stackId;
    }

    public String getNewUserName() {
        return newUserName;
    }

    public String getNewPassword() {
        return newPassword;
    }

    public Platform getCloudPlatform() {
        return cloudPlatform;
    }
}