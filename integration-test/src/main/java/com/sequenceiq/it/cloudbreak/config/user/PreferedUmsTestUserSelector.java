package com.sequenceiq.it.cloudbreak.config.user;

import static com.sequenceiq.it.cloudbreak.config.user.DefaultUserConfig.DEFAULT_USERS;
import static com.sequenceiq.it.cloudbreak.config.user.UmsUserConfig.UMS_USER;

import java.util.Map;

import com.sequenceiq.it.cloudbreak.actor.CloudbreakUser;
import com.sequenceiq.it.cloudbreak.exception.TestFailException;

class PreferedUmsTestUserSelector implements TestUserSelector {

    @Override
    public CloudbreakUser selectByUserLabel(Map<String, TestUserConfig> userConfigs, String label) {
        TestUserConfig umsUsers = userConfigs.get(UMS_USER);
        try {
            return umsUsers.getCloudbreakUserByLabel(label);
        } catch (TestFailException | NullPointerException e) {

        }
        return userConfigs.get(DEFAULT_USERS).getCloudbreakUserByLabel(label);
    }

    @Override
    public CloudbreakUser selectAdminByAccount(Map<String, TestUserConfig> userConfigs, String label) {
        TestUserConfig umsUsers = userConfigs.get(UMS_USER);
        try {
            return umsUsers.getAdminByAccountId(label);
        } catch (TestFailException | NullPointerException e) {

        }
        return userConfigs.get(DEFAULT_USERS).getAdminByAccountId(label);
    }

    @Override
    public CloudbreakUser getDefaultUser(Map<String, TestUserConfig> userConfigs) {
        TestUserConfig umsUsers = userConfigs.get(UMS_USER);
        try {
            return umsUsers.getDefaultUser();
        } catch (TestFailException | NullPointerException e) {

        }
        return userConfigs.get(DEFAULT_USERS).getDefaultUser();
    }
}
