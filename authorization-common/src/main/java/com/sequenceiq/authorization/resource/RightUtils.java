package com.sequenceiq.authorization.resource;

public class RightUtils {

    private RightUtils() {
    }

    public static String getResourceDependentRight(AuthorizationResourceType resource, AuthorizationResourceAction action) {
        return resource.getResourceDependentAuthorizationName() + "/" + action.getAuthorizationName();
    }

    public static String getResourceIndependentRight(AuthorizationResourceType resource, AuthorizationResourceAction action) {
        return resource.getResourceIndependentAuthorizationName() + "/" + action.getAuthorizationName();
    }
}
