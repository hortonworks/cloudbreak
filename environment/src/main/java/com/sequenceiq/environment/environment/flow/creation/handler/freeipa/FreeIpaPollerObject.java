package com.sequenceiq.environment.environment.flow.creation.handler.freeipa;

import com.sequenceiq.freeipa.api.v1.freeipa.stack.FreeIpaV1Endpoint;

public class FreeIpaPollerObject {

    private final Long environmentId;

    private final String environmentCrn;

    private final FreeIpaV1Endpoint freeIpaV1Endpoint;

    public FreeIpaPollerObject(Long environmentId, String environmentCrn, FreeIpaV1Endpoint freeIpaV1Endpoint) {
        this.environmentId = environmentId;
        this.environmentCrn = environmentCrn;
        this.freeIpaV1Endpoint = freeIpaV1Endpoint;
    }

    public String getEnvironmentCrn() {
        return environmentCrn;
    }

    public FreeIpaV1Endpoint getFreeIpaV1Endpoint() {
        return freeIpaV1Endpoint;
    }

    public Long getEnvironmentId() {
        return environmentId;
    }
}
