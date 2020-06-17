package com.sequenceiq.freeipa.api.v1.freeipa.user.doc;

public final class UserOperationDescriptions {
    public static final String SYNC_SINGLE = "Synchronizes a User to the FreeIPA servers";
    public static final String SYNC_ALL = "Synchronizes Groups and Users to the FreeIPA servers";
    public static final String SET_PASSWORD = "Sets the user's password in the FreeIPA servers";
    public static final String SYNC_OPERATION_STATUS = "Gets the status of a sync operation";
    public static final String SYNC_ENVIRONMENT = "Synchronizes all Groups and Users to the FreeIPA servers of an environment";
    public static final String SYNC_ACTOR = "Synchronizes a User and related Groups to the FreeIPA servers of all environments";
    public static final String ENVIRONMENT_USERSYNC_STATE = "Gets the synchronization status of an environment";

    private UserOperationDescriptions() {
    }
}
