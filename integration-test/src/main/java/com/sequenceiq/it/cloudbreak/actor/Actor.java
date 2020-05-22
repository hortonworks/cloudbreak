package com.sequenceiq.it.cloudbreak.actor;

import java.io.IOException;
import java.util.Base64;
import java.util.List;

import org.springframework.util.StringUtils;

import com.fasterxml.jackson.core.type.TypeReference;
import com.sequenceiq.cloudbreak.common.json.JsonUtil;
import com.sequenceiq.cloudbreak.util.FileReaderUtils;
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

    static Actor useRealUmsUser(String key) {
        return testParameter -> {
            String userConfigPath = "ums-users/api-credentials.json";
            try {
                List<CloudbreakUser> users = JsonUtil.readValue(
                        FileReaderUtils.readFileFromClasspathQuietly(userConfigPath), new TypeReference<List<CloudbreakUser>>() {
                });
                return users.stream().filter(u -> u.getDisplayName().equals(key)).findFirst().get();
            } catch (IOException e) {
                throw new RuntimeException("Cannot get UMS user with key: " + key + " from file " + userConfigPath);
            }
        };
    }

    CloudbreakUser acting(TestParameter testParameter);
}
