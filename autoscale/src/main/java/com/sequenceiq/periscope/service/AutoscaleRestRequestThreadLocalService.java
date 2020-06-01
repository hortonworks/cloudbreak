package com.sequenceiq.periscope.service;

import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.common.user.CloudbreakUser;

@Service("RestRequestThreadLocalService")
public class AutoscaleRestRequestThreadLocalService {

    private static final ThreadLocal<Long> REQUESTED_WORKSPACE_ID = new ThreadLocal<>();

    private static final ThreadLocal<CloudbreakUser> CLOUDBREAK_USER = new ThreadLocal<>();

    public void setCloudbreakUser(CloudbreakUser cloudbreakUser, Long workspaceId) {
        CLOUDBREAK_USER.set(cloudbreakUser);
        REQUESTED_WORKSPACE_ID.set(workspaceId);
    }

    public CloudbreakUser getCloudbreakUser() {
        return CLOUDBREAK_USER.get();
    }

    public void removeCloudbreakUser() {
        CLOUDBREAK_USER.remove();
        REQUESTED_WORKSPACE_ID.remove();
    }

    public Long getRequestedWorkspaceId() {
        return REQUESTED_WORKSPACE_ID.get();
    }

}

