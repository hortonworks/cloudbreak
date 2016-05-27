package com.sequenceiq.cloudbreak.reactor.api.event.resource;

import com.sequenceiq.cloudbreak.reactor.api.ClusterPlatformRequest;

public class ClusterCredentialChangeRequest extends ClusterPlatformRequest {
    private final String user;
    private final String password;

    public ClusterCredentialChangeRequest(Long stackId, String user, String password) {
        super(stackId);
        this.user = user;
        this.password = password;
    }

    public String getUser() {
        return user;
    }

    public String getPassword() {
        return password;
    }
}
