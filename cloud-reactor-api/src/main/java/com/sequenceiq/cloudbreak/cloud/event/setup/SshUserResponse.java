package com.sequenceiq.cloudbreak.cloud.event.setup;

import com.sequenceiq.cloudbreak.cloud.context.CloudContext;

public class SshUserResponse<T> {

    private CloudContext cloudContext;
    private String user;

    public SshUserResponse(CloudContext cloudContext, String user) {
        this.cloudContext = cloudContext;
        this.user = user;
    }

    public CloudContext getCloudContext() {
        return cloudContext;
    }

    public String getUser() {
        return user;
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder("SshUserResponse{");
        sb.append("cloudContext=").append(cloudContext);
        sb.append(", user='").append(user).append('\'');
        sb.append('}');
        return sb.toString();
    }
}
