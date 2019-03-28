package com.sequenceiq.cloudbreak.authorization;

public class WorkspaceRightUtils {

    private WorkspaceRightUtils() {

    }

    public static String getRight(WorkspaceResource resource, ResourceAction action) {
        return  "distrox/" + action.name().toLowerCase() + resource.getAuthorizationName();
    }
}
