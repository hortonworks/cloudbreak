package com.sequenceiq.it.cloudbreak.config.user;

import java.util.Map;

import com.sequenceiq.it.cloudbreak.actor.CloudbreakUser;

interface TestUserSelector {
    CloudbreakUser selectByUserLabel(Map<String, TestUserConfig> userConfigs, String label);

    CloudbreakUser selectAdminByAccount(Map<String, TestUserConfig> userConfigs, String label);

    CloudbreakUser getDefaultUser(Map<String, TestUserConfig> userConfigs);
}
