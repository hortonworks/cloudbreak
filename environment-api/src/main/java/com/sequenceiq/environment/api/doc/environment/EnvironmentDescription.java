package com.sequenceiq.environment.api.doc.environment;

public class EnvironmentDescription {
    public static final String ENVIRONMENT_NOTES = "Environment consists of a credential and various other resources and enables users to quickly "
            + "create clusters in given regions in a given cloud provider.";

    public static final String TAG_UPDATE_PERMISSIONS_NOTES = "Reports whether the environment's credential is allowed to propagate tag changes to the "
            + "already created cloud resources, so the result can be shown before tag propagation is enabled. Read-only: nothing is modified and no flow "
            + "is started. Always returns 200; an empty response list means every required permission is granted. A non-empty list contains one entry per "
            + "missing permission with code 404, or a single entry with code 503 when the check itself could not be performed (for example the credential "
            + "is not allowed to read its own permissions) and therefore no individual permission can be named.";

    private EnvironmentDescription() {
    }
}
