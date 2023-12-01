package com.sequenceiq.it.cloudbreak.config.user;

import static com.sequenceiq.it.cloudbreak.config.user.DefaultUserConfig.DEFAULT_USERS;

import java.util.Base64;

import javax.annotation.PostConstruct;
import javax.inject.Inject;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.auth.crn.Crn;
import com.sequenceiq.cloudbreak.util.SanitizerUtil;
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

    private boolean mockedUms;

    @Inject
    private TestUserCreator testUserCreator;

    private CloudbreakUser user;

    @Override
    public CloudbreakUser getCloudbreakUserByLabel(String label) {
        return user;
    }

    @Override
    public CloudbreakUser getAdminByAccountId(String accountId) {
        return mockedUms ? testUserCreator.createAdmin(accountId, "admin") : user;
    }

    @Override
    public CloudbreakUser getDefaultUser() {
        return user;
    }

    @Override
    public boolean isInitialized() {
        return false;
    }

    @PostConstruct
    public void construct() {
        String crn;
        String name;
        String workloadName;

        crn = mockCrnOrConfiguredCrn();
        name = userNameFromCrnOrConfiguredUserName(crn);
        workloadName = workloadUserNameFromCrnOrConfiguredWorkloadUserName(crn);

        user = new CloudbreakUser(accessKey, secretKey, name, crn, "", true, workloadName);
        user.setWorkloadPassword(MOCK_UMS_PASSWORD);
        mockedUms = mockedUms(accessKey);
    }

    private String mockCrnOrConfiguredCrn() {
        String crn;
        try {
            crn = new String(Base64.getDecoder().decode(accessKey));
            Crn.fromString(crn);
        } catch (Exception e) {
            crn = userCrn;
        }
        return crn;
    }

    private String userNameFromCrnOrConfiguredUserName(String crn) {
        String name;
        try {
            name = Crn.fromString(crn).getUserId();
        } catch (Exception e) {
            name = userName;
        }
        return name;
    }

    private String workloadUserNameFromCrnOrConfiguredWorkloadUserName(String crn) {
        String workloadName;

        try {
            workloadName = SanitizerUtil.sanitizeWorkloadUsername(Crn.fromString(crn).getUserId());
        } catch (Exception e) {
            workloadName = workloadUserName;
        }

        return workloadName;
    }

    private boolean mockedUms(String accessKey) {
        try {
            Crn.fromString(new String(Base64.getDecoder().decode(accessKey)));
            return true;
        } catch (Exception e) {
            return false;
        }
    }
}
