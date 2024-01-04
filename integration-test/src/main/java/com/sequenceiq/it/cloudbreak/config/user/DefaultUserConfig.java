package com.sequenceiq.it.cloudbreak.config.user;

import static com.sequenceiq.it.cloudbreak.config.user.DefaultUserConfig.DEFAULT_USERS;

import java.util.Base64;

import jakarta.annotation.PostConstruct;
import jakarta.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.auth.crn.Crn;
import com.sequenceiq.cloudbreak.util.SanitizerUtil;
import com.sequenceiq.it.cloudbreak.actor.CloudbreakUser;
import com.sequenceiq.it.cloudbreak.config.server.CliProfileReaderService;

@Component(DEFAULT_USERS)
class DefaultUserConfig implements TestUserConfig {

    public static final String DEFAULT_USERS = "default-user";

    private static final Logger LOGGER = LoggerFactory.getLogger(DefaultUserConfig.class);

    private static final String MOCK_UMS_PASSWORD = "Password123!";

    @Inject
    private CliProfileReaderService cliProfileReaderService;

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

        user = cliProfileReaderService.getProfileUser().orElseGet(() -> new CloudbreakUser(accessKey, secretKey));

        crn = mockCrnOrConfiguredCrn(user.getAccessKey(), this.userCrn);
        name = userNameFromCrnOrConfiguredUserName(crn,  this.userName);
        workloadName = workloadUserNameFromCrnOrConfiguredWorkloadUserName(crn, this.workloadUserName);

        user.setAdmin(true);
        user.setCrn(crn);
        user.setDisplayName(name);
        user.setWorkloadUserName(workloadName);
        user.setWorkloadPassword(MOCK_UMS_PASSWORD);
        user.setDescription("");
        mockedUms = mockedUms(user.getAccessKey());
    }

    private String mockCrnOrConfiguredCrn(String accessKey, String userCrn) {
        String crn;
        try {
            crn = new String(Base64.getDecoder().decode(accessKey));
            Crn.fromString(crn);
        } catch (Exception e) {
            crn = userCrn;
        }
        return crn;
    }

    private String userNameFromCrnOrConfiguredUserName(String crn, String userName) {
        String name;
        try {
            name = Crn.fromString(crn).getUserId();
        } catch (Exception e) {
            name = userName;
        }
        return name;
    }

    private String workloadUserNameFromCrnOrConfiguredWorkloadUserName(String crn, String workloadUserName) {
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
            LOGGER.info("Test user is for a mocked ums");
            return true;
        } catch (Exception e) {
            LOGGER.info("Test user is ready to use against ums");
            return false;
        }
    }
}
