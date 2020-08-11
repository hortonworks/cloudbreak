package com.sequenceiq.cloudbreak.structuredevent;

import com.sequenceiq.cloudbreak.common.user.CloudbreakUser;

public interface LegacyRestRequestThreadLocalService {
    void setCloudbreakUser(CloudbreakUser cloudbreakUser);

    CloudbreakUser getCloudbreakUser();

    void removeCloudbreakUser();
}
