package com.sequenceiq.it.cloudbreak.newway.actor;

import org.springframework.util.StringUtils;

import com.sequenceiq.it.cloudbreak.newway.CloudbreakTest;
import com.sequenceiq.it.cloudbreak.newway.TestParameter;

public interface Actor {

    static CloudbreakUser defaultUser(TestParameter testParameter) {
        return new CloudbreakUser(testParameter.get(CloudbreakTest.REFRESH_TOKEN));
    }

    static CloudbreakUser secondUser(TestParameter testParameter) {
        String secondaryRefreshToken = testParameter.get(CloudbreakTest.SECONDARY_REFRESH_TOKEN);
        if (StringUtils.isEmpty(secondaryRefreshToken)) {
            throw new IllegalStateException("Add a secondary token to the test: integrationtest.cb.secondarytoken");
        }
        return new CloudbreakUser(testParameter.get(CloudbreakTest.SECONDARY_REFRESH_TOKEN));
    }

    CloudbreakUser acting(TestParameter testParameter);
}
