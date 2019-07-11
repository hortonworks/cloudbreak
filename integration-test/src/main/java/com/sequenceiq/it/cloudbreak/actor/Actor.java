package com.sequenceiq.it.cloudbreak.actor;

import org.springframework.util.StringUtils;

import com.sequenceiq.it.cloudbreak.CloudbreakTest;
import com.sequenceiq.it.TestParameter;

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

    CloudbreakUser acting(TestParameter testParameter);
}
