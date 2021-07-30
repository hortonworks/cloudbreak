package com.sequenceiq.cloudbreak.structuredevent;

import javax.inject.Inject;

import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.auth.CrnUser;
import com.sequenceiq.cloudbreak.auth.security.CrnUserDetailsService;
import com.sequenceiq.cloudbreak.common.user.CloudbreakUser;

@Service
public class CloudbreakRestRequestThreadLocalService implements LegacyRestRequestThreadLocalService {

    private static final ThreadLocal<Long> REQUESTED_WORKSPACE_ID = new ThreadLocal<>();

    private static final ThreadLocal<CloudbreakUser> CLOUDBREAK_USER = new ThreadLocal<>();

    @Inject
    private CrnUserDetailsService crnUserDetailsService;

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
    public void setCloudberUserFromUserCrn(String userCrn) throws UsernameNotFoundException  {
        CrnUser crnUser = crnUserDetailsService.loadUserByUsername(userCrn);
        setCloudbreakUser(crnUser);

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
