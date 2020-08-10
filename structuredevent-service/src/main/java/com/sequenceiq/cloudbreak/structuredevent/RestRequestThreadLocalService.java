package com.sequenceiq.cloudbreak.structuredevent;

import com.sequenceiq.cloudbreak.common.user.CloudbreakUser;

public interface RestRequestThreadLocalService {
    void setCloudbreakUser(CloudbreakUser cloudbreakUser);

    CloudbreakUser getCloudbreakUser();

    void removeCloudbreakUser();
}
