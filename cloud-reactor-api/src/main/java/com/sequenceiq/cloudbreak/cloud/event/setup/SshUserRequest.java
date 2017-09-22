package com.sequenceiq.cloudbreak.cloud.event.setup;

import com.sequenceiq.cloudbreak.cloud.context.CloudContext;
import com.sequenceiq.cloudbreak.cloud.event.CloudPlatformRequest;

public class SshUserRequest<T> extends CloudPlatformRequest<T> {

    private String loginUserName;

    public SshUserRequest(CloudContext cloudContext, String loginUserName) {
        super(cloudContext, null);
        this.loginUserName = loginUserName;
    }

    public String getLoginUserName() {
        return loginUserName;
    }
}
