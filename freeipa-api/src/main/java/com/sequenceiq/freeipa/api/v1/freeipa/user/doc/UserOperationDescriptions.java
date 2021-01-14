package com.sequenceiq.freeipa.api.v1.freeipa.user.doc;

public final class UserOperationDescriptions {
    public static final String SYNC_SINGLE = "Synchronizes a User to the FreeIPA servers";
    public static final String SYNC_ALL = "Synchronizes Groups and Users to the FreeIPA servers";
    public static final String SET_PASSWORD = "Sets the user's password in the FreeIPA servers";
    public static final String SYNC_OPERATION_STATUS = "Gets the status of a sync operation";
    public static final String INTERNAL_SYNC_OPERATION_STATUS =
            "Gets the status of a sync operation by account and id. Used by internal actors.";
    public static final String LAST_SYNC_OPERATION_STATUS = "Gets the status of the last environment sync operation";
    public static final String ENVIRONMENT_USERSYNC_STATE = "Gets the synchronization status of an environment";

    private UserOperationDescriptions() {
    }
}
