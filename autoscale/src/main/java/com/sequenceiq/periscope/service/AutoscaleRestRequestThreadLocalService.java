package com.sequenceiq.periscope.service;

import java.util.Optional;

import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.common.user.CloudbreakUser;

@Service("RestRequestThreadLocalService")
public class AutoscaleRestRequestThreadLocalService {

    private static final ThreadLocal<CloudbreakUser> CLOUDBREAK_USER = new ThreadLocal<>();

    public void setCloudbreakUser(CloudbreakUser cloudbreakUser) {
        CLOUDBREAK_USER.set(cloudbreakUser);
    }

    public CloudbreakUser getCloudbreakUser() {
        return CLOUDBREAK_USER.get();
    }

    public void removeCloudbreakUser() {
        CLOUDBREAK_USER.remove();
    }

    public String getCloudbreakTenant() {
        return Optional.ofNullable(CLOUDBREAK_USER.get()).map(cloudbreakUser -> cloudbreakUser.getTenant()).orElse("");
    }
}

