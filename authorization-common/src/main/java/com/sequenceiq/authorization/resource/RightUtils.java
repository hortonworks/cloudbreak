package com.sequenceiq.authorization.resource;

public class RightUtils {

    private RightUtils() {
    }

    public static String getRight(AuthorizationResourceType resource, AuthorizationResourceAction action) {
        return resource.getResource() + "/" + action.getAction();
    }
}
