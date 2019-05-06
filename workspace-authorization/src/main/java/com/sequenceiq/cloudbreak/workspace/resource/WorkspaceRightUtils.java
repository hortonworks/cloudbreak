package com.sequenceiq.cloudbreak.workspace.resource;

public class WorkspaceRightUtils {

    private WorkspaceRightUtils() {
    }

    public static String getRight(WorkspaceResource resource, ResourceAction action) {
        return  "distrox/" + action.name().toLowerCase() + resource.getAuthorizationName();
    }
}
