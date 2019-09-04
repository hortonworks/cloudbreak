package com.sequenceiq.authorization.resource;

public class RightUtils {

    private RightUtils() {
    }

    public static String getRight(ResourceType resource, ResourceAction action) {
        return resource.getAuthorizationName() + "/" + action.getAuthorizationName();
    }
}
