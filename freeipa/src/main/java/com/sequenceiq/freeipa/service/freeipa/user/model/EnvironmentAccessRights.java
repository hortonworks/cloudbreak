package com.sequenceiq.freeipa.service.freeipa.user.model;

public class EnvironmentAccessRights {
    private final boolean accessEnvironment;

    private final boolean adminFreeIpa;

    public EnvironmentAccessRights(boolean accessEnvironment, boolean adminFreeIpa) {
        this.accessEnvironment = accessEnvironment;
        this.adminFreeIpa = adminFreeIpa;
    }

    public boolean hasEnvironmentAccessRight() {
        return accessEnvironment;
    }

    public boolean hasAdminFreeIpaRight() {
        return adminFreeIpa;
    }
}
