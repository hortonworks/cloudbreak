package com.sequenceiq.cloudbreak.cloud.event.setup;

import com.sequenceiq.cloudbreak.cloud.event.context.CloudContext;

import reactor.rx.Promise;

public class SshUserRequest<T> {

    private CloudContext cloudContext;
    private Promise<T> result;

    public SshUserRequest(CloudContext cloudContext, Promise<T> result) {
        this.cloudContext = cloudContext;
        this.result = result;
    }

    public CloudContext getCloudContext() {
        return cloudContext;
    }

    public Promise<T> getResult() {
        return result;
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder("SshUserRequest{");
        sb.append("cloudContext=").append(cloudContext);
        sb.append(", result=").append(result);
        sb.append('}');
        return sb.toString();
    }
}
