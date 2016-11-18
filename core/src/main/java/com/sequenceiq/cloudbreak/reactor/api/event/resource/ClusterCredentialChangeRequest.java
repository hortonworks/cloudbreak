package com.sequenceiq.cloudbreak.reactor.api.event.resource;

import com.sequenceiq.cloudbreak.reactor.api.ClusterPlatformRequest;

public class ClusterCredentialChangeRequest extends ClusterPlatformRequest {
    private final String user;

    private final String password;

    private final Type type;

    private ClusterCredentialChangeRequest(Long stackId, String user, String password, Type type) {
        super(stackId);
        this.user = user;
        this.password = password;
        this.type = type;
    }

    public static ClusterCredentialChangeRequest replaceUserRequest(Long stackId, String user, String password) {
        return new ClusterCredentialChangeRequest(stackId, user, password, Type.REPLACE);
    }

    public static ClusterCredentialChangeRequest changePasswordRequest(Long stackId, String password) {
        return new ClusterCredentialChangeRequest(stackId, null, password, Type.UPDATE);
    }

    public String getUser() {
        return user;
    }

    public String getPassword() {
        return password;
    }

    public Type getType() {
        return type;
    }

    public enum Type {
        REPLACE, UPDATE
    }
}
