package com.sequenceiq.freeipa.api.v1.freeipa.user.doc;

public final class UserOperationDescriptions {
    public static final String SYNC_SINGLE = "Synchronizes a User to the FreeIPA servers";
    public static final String SYNC_ALL = "Synchronizes Groups and Users to the FreeIPA servers";
    public static final String SYNC_STATUS = "Gets the status of synchronization operation";
    public static final String SET_PASSWORD = "Sets the user's password in the FreeIPA servers";
    public static final String CREATE = "Creates Groups and Users in the FreeIPA servers";

    private UserOperationDescriptions() {
    }
}
