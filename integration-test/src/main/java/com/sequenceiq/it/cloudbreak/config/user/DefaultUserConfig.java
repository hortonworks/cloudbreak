package com.sequenceiq.it.cloudbreak.config.user;

import static com.sequenceiq.it.cloudbreak.config.user.DefaultUserConfig.DEFAULT_USERS;

import java.util.Base64;

import javax.annotation.PostConstruct;
import javax.inject.Inject;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.auth.crn.Crn;
import com.sequenceiq.it.cloudbreak.actor.CloudbreakUser;

@Component(DEFAULT_USERS)
class DefaultUserConfig implements TestUserConfig {
    public static final String DEFAULT_USERS = "default-user";

    private static final String MOCK_UMS_PASSWORD = "Password123!";

    @Value("${integrationtest.user.accesskey:}")
    private String accessKey;

    @Value("${integrationtest.user.secretkey:}")
    private String secretKey;

    @Value("${integrationtest.user.crn:}")
    private String userCrn;

    @Value("${integrationtest.user.name:}")
    private String userName;

    @Value("${integrationtest.user.workloadUserName:}")
    private String workloadUserName;

    @Inject
    private TestUserCreator testUserCreator;

    private CloudbreakUser user;

    @Override
    public CloudbreakUser getCloudbreakUserByLabel(String label) {
        return user;
    }

    @Override
    public CloudbreakUser getAdminByAccountId(String accountId) {
        return testUserCreator.createAdmin(accountId, "admin");
    }

    @Override
    public CloudbreakUser getDefaultUser() {
        return user;
    }

    @PostConstruct
    public void construct() {
        String crn;
        String name;
        if (userCrn == null || userCrn.isEmpty()) {
            try {
                crn = new String(Base64.getDecoder().decode(accessKey));
            } catch (Exception e) {
                crn = "";
            }
        } else {
            crn = userCrn;
        }
        if (userName == null || userName.isEmpty()) {
            try {
                name = Crn.fromString(crn).getUserId();
            } catch (Exception e) {
                name = "";
            }
        } else {
            name = userName;
        }
        user = new CloudbreakUser(accessKey, secretKey, name, crn, "", true, workloadUserName);
        user.setWorkloadPassword(MOCK_UMS_PASSWORD);
    }
}
