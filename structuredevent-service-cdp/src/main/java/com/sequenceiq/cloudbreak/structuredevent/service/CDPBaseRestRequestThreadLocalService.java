package com.sequenceiq.cloudbreak.structuredevent.service;

import com.sequenceiq.cloudbreak.common.user.CloudbreakUser;

public interface CDPBaseRestRequestThreadLocalService {
    void setCloudbreakUser(CloudbreakUser cloudbreakUser);

    CloudbreakUser getCloudbreakUser();

    void removeCloudbreakUser();
}
