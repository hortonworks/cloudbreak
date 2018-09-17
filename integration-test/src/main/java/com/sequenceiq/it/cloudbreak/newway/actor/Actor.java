package com.sequenceiq.it.cloudbreak.newway.actor;

import org.springframework.util.StringUtils;

import com.sequenceiq.it.cloudbreak.newway.CloudbreakTest;
import com.sequenceiq.it.cloudbreak.newway.TestParameter;

public interface Actor {

    static CloudbreakUser defaultUser(TestParameter testParameter) {
        return new CloudbreakUser(testParameter.get(CloudbreakTest.USER), testParameter.get(CloudbreakTest.PASSWORD));
    }

    static CloudbreakUser secondUser(TestParameter testParameter) {
        String username = testParameter.get(CloudbreakTest.SECOND_USER);
        if (StringUtils.isEmpty(username)) {
            throw new IllegalStateException("Add a second user to the test: integrationtest.uaa.secondUser and integrationtest.uaa.secondPassword or with -D");
        }
        return new CloudbreakUser(username, testParameter.get(CloudbreakTest.PASSWORD));
    }

    CloudbreakUser acting(TestParameter testParameter);
}
