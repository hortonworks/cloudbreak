package com.sequenceiq.it.cloudbreak.config.user;

import java.util.Base64;
import java.util.Locale;

import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import com.sequenceiq.it.cloudbreak.actor.CloudbreakUser;

@Component
public class TestUserCreator {

    @Value("${integrationtest.user.secretkey:}")
    private String secretKey;

    public CloudbreakUser create(String tenantName, String username) {
        String crn = String.format("crn:cdp:iam:us-west-1:%s:user:%s", tenantName, username);
        String accessKey = Base64.getEncoder().encodeToString(crn.getBytes());
        checkNonEmpty("integrationtest.user.secretkey", secretKey);
        return new CloudbreakUser(accessKey, secretKey, username + " at tenant " + tenantName, crn);
    }

    public CloudbreakUser createAdmin(String tenantName, String username) {
        String crn = String.format("crn:cdp:iam:us-west-1:%s:user:%s", tenantName, username);
        String accessKey = Base64.getEncoder().encodeToString(crn.getBytes());
        checkNonEmpty("integrationtest.user.secretkey", secretKey);
        return new CloudbreakUser(accessKey, secretKey, username + " at tenant " + tenantName, crn, "", true, username);
    }

    private void checkNonEmpty(String name, String value) {
        if (StringUtils.isBlank(value)) {
            throw new NullPointerException(String.format("Following variable must be set whether as environment variables or (test) application.yml:: %s",
                    name.replaceAll("\\.", "_").toUpperCase(Locale.ROOT)));
        }
    }
}
