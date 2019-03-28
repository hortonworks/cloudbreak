package com.sequenceiq.cloudbreak.service;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.common.model.user.CloudbreakUser;

@Service("RestRequestThreadLocalService")
public class CloudbreakRestRequestThreadLocalService implements RestRequestThreadLocalService {

    private static final ThreadLocal<Long> REQUESTED_WORKSPACE_ID = new ThreadLocal<>();

    private static final ThreadLocal<CloudbreakUser> CLOUDBREAK_USER = new ThreadLocal<>();

    public void setRequestedWorkspaceId(Long workspaceId) {
        REQUESTED_WORKSPACE_ID.set(workspaceId);
    }

    public Long getRequestedWorkspaceId() {
        return REQUESTED_WORKSPACE_ID.get();
    }

    public void removeRequestedWorkspaceId() {
        REQUESTED_WORKSPACE_ID.remove();
    }

    @Override
    public void setCloudbreakUser(CloudbreakUser cloudbreakUser) {
        CLOUDBREAK_USER.set(cloudbreakUser);
    }

    @Override
    public CloudbreakUser getCloudbreakUser() {
        return CLOUDBREAK_USER.get();
    }

    @Override
    public void removeCloudbreakUser() {
        CLOUDBREAK_USER.remove();
    }

    @PreAuthorize("#oauth2.hasScope('cloudbreak.autoscale')")
    public void setCloudbreakUserByUsernameAndTenant(String userId, String tenant) {
        CLOUDBREAK_USER.set(new CloudbreakUser(userId, "", "", "", tenant));
    }
}
