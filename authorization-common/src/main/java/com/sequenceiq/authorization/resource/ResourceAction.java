package com.sequenceiq.authorization.resource;

import java.util.Arrays;
import java.util.Optional;

import org.apache.commons.lang3.StringUtils;

public enum ResourceAction {
    READ("read"),
    WRITE("write"),
    // manage action is obsolete, used only in workspace authz
    MANAGE("manage");

    private String authorizationName;

    ResourceAction(String authorizationName) {
        this.authorizationName = authorizationName;
    }

    public String getAuthorizationName() {
        return authorizationName;
    }

    public static Optional<ResourceAction> getByName(String name) {
        return Arrays.stream(ResourceAction.values())
                .filter(resource -> StringUtils.equals(resource.getAuthorizationName(), name))
                .findAny();
    }
}
