package com.sequenceiq.cloudbreak.auth.security.authentication;

import com.sequenceiq.cloudbreak.common.user.CloudbreakUser;

public class DisabledAuthCbUserProvider {

    public CloudbreakUser getCloudbreakUser() {
        return new CloudbreakUser("disabledAuth",
                "crn:altus:iam:us-west-1:cloudera:user:disabledAuth",
                "disabledAuth",
                "disabledAuth@cloudera.com",
                "cloudera");
    }
}
