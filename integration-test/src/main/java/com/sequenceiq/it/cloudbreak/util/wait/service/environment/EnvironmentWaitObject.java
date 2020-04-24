package com.sequenceiq.it.cloudbreak.util.wait.service.environment;

import com.sequenceiq.environment.api.v1.environment.endpoint.EnvironmentEndpoint;
import com.sequenceiq.environment.api.v1.environment.model.response.EnvironmentStatus;
import com.sequenceiq.it.cloudbreak.EnvironmentClient;

public class EnvironmentWaitObject {

    private final EnvironmentClient client;

    private final String crn;

    private final EnvironmentStatus desiredStatus;

    public EnvironmentWaitObject(EnvironmentClient environmentClient, String environmentCrn, EnvironmentStatus desiredStatus) {
        this.client = environmentClient;
        this.crn = environmentCrn;
        this.desiredStatus = desiredStatus;
    }

    public EnvironmentEndpoint getEndpoint() {
        return client.getEnvironmentClient().environmentV1Endpoint();
    }

    public String getCrn() {
        return crn;
    }

    public EnvironmentStatus getDesiredStatus() {
        return desiredStatus;
    }
}
