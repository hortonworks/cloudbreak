package com.sequenceiq.environment.environment.flow.creation.handler.freeipa;

public class FreeIpaPollerObject {

    private final Long environmentId;

    private final String environmentCrn;

    public FreeIpaPollerObject(Long environmentId, String environmentCrn) {
        this.environmentId = environmentId;
        this.environmentCrn = environmentCrn;
    }

    public String getEnvironmentCrn() {
        return environmentCrn;
    }

    public Long getEnvironmentId() {
        return environmentId;
    }
}
