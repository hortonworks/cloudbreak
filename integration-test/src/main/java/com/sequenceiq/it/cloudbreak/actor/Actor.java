package com.sequenceiq.it.cloudbreak.actor;

import java.util.Base64;

import org.springframework.util.StringUtils;

import com.sequenceiq.it.TestParameter;
import com.sequenceiq.it.cloudbreak.CloudbreakTest;

public interface Actor {

    static CloudbreakUser defaultUser(TestParameter testParameter) {
        return new CloudbreakUser(testParameter.get(CloudbreakTest.ACCESS_KEY), testParameter.get(CloudbreakTest.SECRET_KEY));
    }

    static CloudbreakUser secondUser(TestParameter testParameter) {
        String secondaryAccessKey = testParameter.get(CloudbreakTest.SECONDARY_ACCESS_KEY);
        String secondarySecretKey = testParameter.get(CloudbreakTest.SECONDARY_SECRET_KEY);
        if (StringUtils.isEmpty(secondaryAccessKey)) {
            throw new IllegalStateException("Add a secondary accessKey to the test: integrationtest.cb.secondary.accesskey");
        }
        if (StringUtils.isEmpty(secondarySecretKey)) {
            throw new IllegalStateException("Add a secondary secretKey to the test: integrationtest.cb.secondary.secretkey");
        }
        return new CloudbreakUser(testParameter.get(CloudbreakTest.SECONDARY_ACCESS_KEY), testParameter.get(CloudbreakTest.SECONDARY_SECRET_KEY));
    }

    static Actor create(String tenantName, String username) {
        return testParameter -> {
            String secretKey = testParameter.get(CloudbreakTest.SECRET_KEY);
            String crn = String.format("crn:cdp:iam:us-west-1:%s:user:%s", tenantName, username);
            String accessKey = Base64.getEncoder().encodeToString(crn.getBytes());
            return new CloudbreakUser(accessKey, secretKey, username + " at tenant " + tenantName);
        };
    }

    CloudbreakUser acting(TestParameter testParameter);
}
