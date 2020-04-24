package com.sequenceiq.it.cloudbreak.util.wait.service.freeipa;

import com.sequenceiq.freeipa.api.v1.freeipa.stack.FreeIpaV1Endpoint;
import com.sequenceiq.freeipa.api.v1.freeipa.stack.model.common.Status;
import com.sequenceiq.it.cloudbreak.FreeIPAClient;

public class FreeIpaWaitObject {

    private final FreeIPAClient client;

    private final String environmentCrn;

    private final Status desiredStatus;

    public FreeIpaWaitObject(FreeIPAClient freeIPAClient, String environmentCrn, Status desiredStatus) {
        this.client = freeIPAClient;
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

    public String getFreeIpaCrn() {
        return client.getFreeIpaClient().getFreeIpaV1Endpoint().describe(getEnvironmentCrn()).getCrn();
    }
}
