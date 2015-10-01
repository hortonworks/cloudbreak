package com.sequenceiq.cloudbreak.service.cluster.event;

import com.sequenceiq.cloudbreak.common.type.CloudPlatform;

public class ClusterUserNamePasswordUpdateRequest {

    private Long stackId;
    private String newUserName;
    private String newPassword;
    private CloudPlatform cloudPlatform;

    public ClusterUserNamePasswordUpdateRequest(long stackId, String newUserName, String newPassword, CloudPlatform cloudPlatform) {
        this.stackId = stackId;
        this.newUserName = newUserName;
        this.newPassword = newPassword;
        this.cloudPlatform = cloudPlatform;
    }

    public long getStackId() {
        return stackId;
    }

    public void setStackId(long stackId) {
        this.stackId = stackId;
    }

    public String getNewUserName() {
        return newUserName;
    }

    public String getNewPassword() {
        return newPassword;
    }

    public CloudPlatform getCloudPlatform() {
        return cloudPlatform;
    }
}