package com.sequenceiq.it.cloudbreak.util.wait.service.freeipa;

import com.sequenceiq.freeipa.api.v1.freeipa.stack.FreeIpaV1Endpoint;
import com.sequenceiq.freeipa.api.v1.freeipa.stack.model.common.Status;
import com.sequenceiq.it.cloudbreak.FreeIpaClient;

public class FreeIpaWaitObject {

    private final FreeIpaClient client;

    private final String environmentCrn;

    private final Status desiredStatus;

    public FreeIpaWaitObject(FreeIpaClient freeIpaClient, String environmentCrn, Status desiredStatus) {
        this.client = freeIpaClient;
        this.environmentCrn = environmentCrn;
        this.desiredStatus = desiredStatus;
    }

    public FreeIpaV1Endpoint getEndpoint() {
        return client.getFreeIpaClient().getFreeIpaV1Endpoint();
    }

    public String getEnvironmentCrn() {
        return environmentCrn;
    }

    public Status getDesiredStatus() {
        return desiredStatus;
    }
}
