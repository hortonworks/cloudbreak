package com.sequenceiq.it.cloudbreak.config.user;

import java.util.Map;

import com.sequenceiq.it.cloudbreak.actor.CloudbreakUser;

class DefaultTestUserSelector implements TestUserSelector {
    private String configName;

    DefaultTestUserSelector(String configName) {
        this.configName = configName;
    }

    @Override
    public CloudbreakUser selectByUserLabel(Map<String, TestUserConfig> userConfigs, String label) {
        return userConfigs.get(configName).getCloudbreakUserByLabel(label);
    }

    @Override
    public CloudbreakUser selectAdminByAccount(Map<String, TestUserConfig> userConfigs, String account) {
        return userConfigs.get(configName).getAdminByAccountId(account);
    }

    @Override
    public CloudbreakUser getDefaultUser(Map<String, TestUserConfig> userConfigs) {
        return userConfigs.get(configName).getDefaultUser();
    }
}
