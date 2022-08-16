package com.sequenceiq.cloudbreak.structuredevent;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.auth.ThreadBasedUserCrnProvider;
import com.sequenceiq.cloudbreak.common.user.CloudbreakUser;

@Service
public class CloudbreakRestRequestThreadLocalService implements LegacyRestRequestThreadLocalService {

    private static final Logger LOGGER = LoggerFactory.getLogger(CloudbreakRestRequestThreadLocalService.class);

    private static final ThreadLocal<Long> REQUESTED_WORKSPACE_ID = new ThreadLocal<>();

    private static final ThreadLocal<CloudbreakUser> CLOUDBREAK_USER = new ThreadLocal<>();

    public Long getRequestedWorkspaceId() {
        return REQUESTED_WORKSPACE_ID.get();
    }

    public void setRequestedWorkspaceId(Long workspaceId) {
        REQUESTED_WORKSPACE_ID.set(workspaceId);
    }

    public void removeRequestedWorkspaceId() {
        REQUESTED_WORKSPACE_ID.remove();
    }

    @Override
    public CloudbreakUser getCloudbreakUser() {
        return CLOUDBREAK_USER.get();
    }

    @Override
    public void setCloudbreakUser(CloudbreakUser cloudbreakUser) {
        CLOUDBREAK_USER.set(cloudbreakUser);
    }

    @Override
    public void removeCloudbreakUser() {
        CLOUDBREAK_USER.remove();
    }

    public void setCloudbreakUserByUsernameAndTenant(String userId, String tenant) {
        CLOUDBREAK_USER.set(new CloudbreakUser(userId, "", "", "", tenant));
    }

    public void setWorkspaceId(Long workspaceId) {
        if (workspaceId != null) {
            LOGGER.debug("Set workspace id thread local variable to '{}'. Value was '{}'.",
                    workspaceId,
                    getRequestedWorkspaceId());
            setRequestedWorkspaceId(workspaceId);
        } else {
            LOGGER.error("Workspace is missing, unable to set the thread local variable to a valid workspace id. " +
                    "Overwrite the previous value ('{}') with null.", getRequestedWorkspaceId());
            setRequestedWorkspaceId(null);
        }
    }

    public String getRestThreadLocalContextAsString() {
        return String.format("CloudbreakUser: %s WorkspaceId: %s", CLOUDBREAK_USER.get(), REQUESTED_WORKSPACE_ID.get());
    }

    public String getAccountId() {
        return ThreadBasedUserCrnProvider.getAccountId();
    }

    public String getUserCrn() {
        return ThreadBasedUserCrnProvider.getUserCrn();
    }
}
