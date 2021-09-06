package com.sequenceiq.cloudbreak.structuredevent;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.common.user.CloudbreakUser;
import com.sequenceiq.cloudbreak.workspace.model.Workspace;

@Service
public class CloudbreakRestRequestThreadLocalService implements LegacyRestRequestThreadLocalService {

    private static final Logger LOGGER = LoggerFactory.getLogger(CloudbreakRestRequestThreadLocalService.class);

    private static final ThreadLocal<Long> REQUESTED_WORKSPACE_ID = new ThreadLocal<>();

    private static final ThreadLocal<CloudbreakUser> CLOUDBREAK_USER = new ThreadLocal<>();

    public void setRequestedWorkspaceId(Long workspaceId) {
        REQUESTED_WORKSPACE_ID.set(workspaceId);
    }

    public Long getRequestedWorkspaceId() {
        return REQUESTED_WORKSPACE_ID.get();
    }

    public void setWorkspaceId(Workspace workspace) {
        if (workspace != null) {
            LOGGER.debug("Set workspace id thread local variable to '{}'. Value was '{}'.",
                    workspace.getId(),
                    getRequestedWorkspaceId());
            setRequestedWorkspaceId(workspace.getId());
        } else {
            LOGGER.error("Workspace is missing, unable to set the thread local variable to a valid workspace id. " +
                    "Overwrite the previous value ('{}') with null.", getRequestedWorkspaceId());
            setRequestedWorkspaceId(null);
        }
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

    public void setCloudbreakUserByUsernameAndTenant(String userId, String tenant) {
        CLOUDBREAK_USER.set(new CloudbreakUser(userId, "", "", "", tenant));
    }
}
