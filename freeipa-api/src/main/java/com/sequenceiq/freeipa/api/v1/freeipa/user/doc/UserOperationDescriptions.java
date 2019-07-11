package com.sequenceiq.freeipa.api.v1.freeipa.user.doc;

public final class UserOperationDescriptions {
    public static final String SYNC_SINGLE = "Synchronizes a User to the FreeIPA servers";
    public static final String SYNC_ALL = "Synchronizes Groups and Users to the FreeIPA servers";
    public static final String SET_PASSWORD = "Sets the user's password in the FreeIPA servers";
    public static final String SYNC_OPERATION_STATUS = "Gets the status of a sync operation";

    private UserOperationDescriptions() {
    }
}
