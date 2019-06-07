package com.sequenceiq.environment.environment.flow.creation.handler.freeipa;

import com.sequenceiq.freeipa.api.v1.freeipa.stack.FreeIpaV1Endpoint;

public class FreeIpaPollerObject {

    private final String environmentCrn;

    private final FreeIpaV1Endpoint freeIpaV1Endpoint;

    public FreeIpaPollerObject(String environmentCrn, FreeIpaV1Endpoint freeIpaV1Endpoint) {
        this.environmentCrn = environmentCrn;
        this.freeIpaV1Endpoint = freeIpaV1Endpoint;
    }

    public String getEnvironmentCrn() {
        return environmentCrn;
    }

    public FreeIpaV1Endpoint getFreeIpaV1Endpoint() {
        return freeIpaV1Endpoint;
    }
}
