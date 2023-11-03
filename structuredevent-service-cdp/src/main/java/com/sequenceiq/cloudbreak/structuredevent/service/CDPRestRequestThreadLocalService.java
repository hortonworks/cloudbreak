package com.sequenceiq.cloudbreak.structuredevent.service;

import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.auth.ThreadBasedUserCrnProvider;
import com.sequenceiq.cloudbreak.common.user.CloudbreakUser;

/**
 * @deprecated please use {@link ThreadBasedUserCrnProvider} if possible
 */
@Deprecated
@Service("RestRequestThreadLocalService")
public class CDPRestRequestThreadLocalService implements CDPBaseRestRequestThreadLocalService {

    private static final ThreadLocal<CloudbreakUser> CLOUDBREAK_USER = new ThreadLocal<>();

    @Deprecated
    @Override
    public void setCloudbreakUser(CloudbreakUser cloudbreakUser) {
        CLOUDBREAK_USER.set(cloudbreakUser);
    }

    /**
     * @deprecated please use {@link ThreadBasedUserCrnProvider#getUserCrn()} if possible
     */
    @Deprecated
    @Override
    public CloudbreakUser getCloudbreakUser() {
        return CLOUDBREAK_USER.get();
    }

    @Deprecated
    @Override
    public void removeCloudbreakUser() {
        CLOUDBREAK_USER.remove();
    }
}
