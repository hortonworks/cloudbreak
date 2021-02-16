package com.sequenceiq.it.cloudbreak.actor;

import java.util.Base64;

import javax.inject.Inject;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.sequenceiq.it.TestParameter;
import com.sequenceiq.it.cloudbreak.CloudbreakTest;

@Component
public class CloudbreakActor extends CloudbreakUserCache implements Actor {

    private static final Logger LOGGER = LoggerFactory.getLogger(CloudbreakActor.class);

    @Inject
    private TestParameter testParameter;

    @Override
    public CloudbreakUser defaultUser() {
        String accessKey = testParameter.get(CloudbreakTest.ACCESS_KEY);
        String secretKey = testParameter.get(CloudbreakTest.SECRET_KEY);
        checkNonEmpty("integrationtest.user.accesskey", accessKey);
        checkNonEmpty("integrationtest.user.secretkey", secretKey);
        return new CloudbreakUser(accessKey, secretKey);
    }

    @Override
    public CloudbreakUser secondUser() {
        String secondaryAccessKey = testParameter.get(CloudbreakTest.SECONDARY_ACCESS_KEY);
        String secondarySecretKey = testParameter.get(CloudbreakTest.SECONDARY_SECRET_KEY);
        checkNonEmpty("integrationtest.cb.secondary.accesskey", secondaryAccessKey);
        checkNonEmpty("integrationtest.cb.secondary.secretkey", secondarySecretKey);
        return new CloudbreakUser(secondaryAccessKey, secondarySecretKey);
    }

    @Override
    public CloudbreakUser create(String tenantName, String username) {
        String secretKey = testParameter.get(CloudbreakTest.SECRET_KEY);
        String crn = String.format("crn:cdp:iam:us-west-1:%s:user:%s", tenantName, username);
        String accessKey = Base64.getEncoder().encodeToString(crn.getBytes());
        checkNonEmpty("integrationtest.user.secretkey", secretKey);
        return new CloudbreakUser(accessKey, secretKey, username + " at tenant " + tenantName);
    }

    @Override
    public CloudbreakUser useRealUmsUser(String key) {
        LOGGER.info("Getting the requested real UMS user by key:: {}", key);
        return getUserByDisplayName(key);
    }

    private void checkNonEmpty(String name, String value) {
        if (StringUtils.isBlank(value)) {
            throw new NullPointerException(String.format("Following variable must be set whether as environment variables or (test) application.yml:: %s",
                    name.replaceAll("\\.", "_").toUpperCase()));
        }
    }
}