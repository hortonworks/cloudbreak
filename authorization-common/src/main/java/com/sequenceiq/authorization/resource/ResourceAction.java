package com.sequenceiq.authorization.resource;

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
}
