package com.sequenceiq.periscope.service;

import java.util.Optional;

import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.auth.ThreadBasedUserCrnProvider;
import com.sequenceiq.cloudbreak.common.user.CloudbreakUser;

/**
 * @deprecated please use {@link ThreadBasedUserCrnProvider} if possible
 */
@Deprecated
@Service("RestRequestThreadLocalService")
public class AutoscaleRestRequestThreadLocalService {

    private static final ThreadLocal<CloudbreakUser> CLOUDBREAK_USER = new ThreadLocal<>();

    public void setCloudbreakUser(CloudbreakUser cloudbreakUser) {
        CLOUDBREAK_USER.set(cloudbreakUser);
    }

    /**
     * @deprecated please use {@link ThreadBasedUserCrnProvider#getUserCrn()} if possible
     */
    @Deprecated
    public CloudbreakUser getCloudbreakUser() {
        return CLOUDBREAK_USER.get();
    }

    @Deprecated
    public void removeCloudbreakUser() {
        CLOUDBREAK_USER.remove();
    }

    /**
     * @deprecated please use {@link ThreadBasedUserCrnProvider#getAccountId()} if possible
     */
    @Deprecated
    public String getCloudbreakTenant() {
        return Optional.ofNullable(CLOUDBREAK_USER.get()).map(cloudbreakUser -> cloudbreakUser.getTenant()).orElse("");
    }
}

