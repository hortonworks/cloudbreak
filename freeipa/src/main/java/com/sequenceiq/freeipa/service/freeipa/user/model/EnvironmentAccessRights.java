package com.sequenceiq.freeipa.service.freeipa.user.model;

public class EnvironmentAccessRights {
    private final boolean accessEnvironment;

    private final boolean adminFreeIPA;

    public EnvironmentAccessRights(boolean accessEnvironment, boolean adminFreeIPA) {
        this.accessEnvironment = accessEnvironment;
        this.adminFreeIPA = adminFreeIPA;
    }

    public boolean hasEnvironmentAccessRight() {
        return accessEnvironment;
    }

    public boolean hasAdminFreeIPARight() {
        return adminFreeIPA;
    }
}
