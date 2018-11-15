package com.sequenceiq.cloudbreak.service;

import com.sequenceiq.cloudbreak.common.model.user.CloudbreakUser;

public interface RestRequestThreadLocalService {
    void setCloudbreakUser(CloudbreakUser cloudbreakUser);

    CloudbreakUser getCloudbreakUser();

    void removeCloudbreakUser();
}
