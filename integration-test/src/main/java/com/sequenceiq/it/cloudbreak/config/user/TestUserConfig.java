package com.sequenceiq.it.cloudbreak.config.user;

import com.sequenceiq.it.cloudbreak.actor.CloudbreakUser;

interface TestUserConfig {

    CloudbreakUser getCloudbreakUserByLabel(String label);

    CloudbreakUser getAdminByAccountId(String accountId);

    CloudbreakUser getDefaultUser();
}
