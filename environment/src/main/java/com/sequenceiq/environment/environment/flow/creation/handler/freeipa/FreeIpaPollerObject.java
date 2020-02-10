package com.sequenceiq.environment.environment.flow.creation.handler.freeipa;

public class FreeIpaPollerObject {

    private final Long environmentId;

    private final String environmentCrn;

    private final boolean createFreeipa;

    public FreeIpaPollerObject(Long environmentId, String environmentCrn, boolean createFreeipa) {
        this.environmentId = environmentId;
        this.environmentCrn = environmentCrn;
        this.createFreeipa = createFreeipa;
    }

    public String getEnvironmentCrn() {
        return environmentCrn;
    }

    public Long getEnvironmentId() {
        return environmentId;
    }

    public boolean isCreateFreeipa() {
        return createFreeipa;
    }
}
